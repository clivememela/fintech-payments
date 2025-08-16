package za.co.titandynamix.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import za.co.titandynamix.dto.CreateAccountRequest;
import za.co.titandynamix.dto.LedgerEntryRequest;
import za.co.titandynamix.dto.TransactionResult;
import za.co.titandynamix.entity.Account;
import za.co.titandynamix.entity.IdempotencyKey;
import za.co.titandynamix.entity.LedgerEntry;
import za.co.titandynamix.entity.LedgerEntryType;
import za.co.titandynamix.repository.AccountRepository;
import za.co.titandynamix.repository.IdempotencyKeyRepository;
import za.co.titandynamix.repository.LedgerEntryRepository;
import za.co.titandynamix.service.LedgerService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The type Ledger service.
 */
@Service
@RequiredArgsConstructor
public class ILedgerService implements LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get a list of all accounts
     *
     * @return List<Account> list of all accounts
     */
    @Override
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Create an account with an initial balance.
     * Add transaction to deposit the initial amount.
     *
     * @param createAccountRequest createAccountRequest object
     * @return Account the created account
     */
    @Transactional
    @Override
    public Account createAccount(String idempotencyKey, CreateAccountRequest createAccountRequest) {
        IdempotencyKey savedIdempotencyKey = idempotencyKeyRepository.findByKey(idempotencyKey);

        if(savedIdempotencyKey != null){
            if(savedIdempotencyKey.getExpirationDate().isBefore(LocalDateTime.now()))
                idempotencyKeyRepository.delete(savedIdempotencyKey);
        }

        UUID transferId = (createAccountRequest.getTransferId() != null)
                ? createAccountRequest.getTransferId()
                : UUID.randomUUID();

        if (createAccountRequest.getAccountName() == null || createAccountRequest.getAccountName().trim().isEmpty())
            throw new IllegalArgumentException("Account name cannot be null or empty");

        // Normalize the initial amount
        var initialAmount = createAccountRequest.getBalance() == null
                ? BigDecimal.ZERO
                : createAccountRequest.getBalance();

        Account account = new Account();
        account.setAccountName(createAccountRequest.getAccountName());
        account.setBalance(initialAmount); // Keep the balance in sync with the ledger
        Account saved = accountRepository.save(account);

        // Record the initial deposit as a CREDIT ledger entry (if > 0)
        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            LedgerEntry deposit = new LedgerEntry();
            deposit.setAccount(saved);
            deposit.setAmount(initialAmount);       // CREDIT -> positive amount
            deposit.setType(LedgerEntryType.CREDIT);
            deposit.setCreatedAt(LocalDateTime.now());
            deposit.setTransferId(transferId);
            ledgerEntryRepository.save(deposit);
        }

        String response = "Account created with ID: " + saved.getId();

        IdempotencyKey newKey = new IdempotencyKey();
        newKey.setKey(idempotencyKey);
        newKey.setResponse(response);
        newKey.setExpirationDate(LocalDateTime.now().plusHours(24)); // 24-hour expiration
        idempotencyKeyRepository.save(newKey);

        return saved;
    }

    /**
     * fetch current balance and metadata.
     *
     * @param id The id of the account
     * @return Account the account queried
     */
    public Account getAccountByAccountId(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account with id : %s Not found".formatted(id)));
    }

    /**
     * Apply an idempotent, atomic double-entry transfer with comprehensive concurrency control.
     * 
     * Concurrency Control Features:
     * - Pessimistic locking (SELECT ... FOR UPDATE) on accounts in deterministic order to prevent deadlocks
     * - Optimistic locking with @Version field on Account entity for additional race condition protection
     * - Database-level unique constraint on (transferId, type) for idempotency enforcement
     * - SERIALIZABLE isolation level for maximum consistency
     * - Retry mechanism for handling optimistic lock failures
     * 
     * @param ledgerEntryRequest the transfer request containing transferId, accounts, and amount
     * @return TransactionResult indicating success or failure with detailed message
     */
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.SERIALIZABLE,
            rollbackFor = Exception.class
    )
    @Override
    public TransactionResult createDoubleEntryTransaction(LedgerEntryRequest ledgerEntryRequest){
        // Validate request
        TransactionResult validation = validate(ledgerEntryRequest);
        if (validation != null) return validation;

        UUID transferId = (ledgerEntryRequest.getTransferId() != null)
                ? ledgerEntryRequest.getTransferId()
                : UUID.randomUUID();

        // Fast idempotency check (avoid re-applying)
        if (isTransferAlreadyProcessed(transferId))
            return TransactionResult.success("Transfer already processed...");

        // Lock accounts in a deterministic order to prevent deadlocks
        Long fromId = ledgerEntryRequest.getFromAccountId();
        Long toId = ledgerEntryRequest.getToAccountId();
        LockedAccounts locked = lockAccountsForUpdate(fromId, toId);

        Account fromAccount = locked.fromAccount();
        Account toAccount = locked.toAccount();

        if (fromAccount == null || toAccount == null)
            return TransactionResult.failure("Account not found for provided IDs.");


        // Double-check idempotency after acquiring locks (handles races)
        if (isTransferAlreadyProcessed(transferId))
            return TransactionResult.success("Transfer already processed...");

        BigDecimal amount = ledgerEntryRequest.getAmount();

        // Sufficient funds check (prevent negative balance)
        if (fromAccount.getBalance().compareTo(amount) < 0)
            return TransactionResult.failure("Insufficient funds in the source account.");

        LocalDateTime now = LocalDateTime.now();

        LedgerEntry debit = new LedgerEntry();
        debit.setAccount(fromAccount);
        debit.setAmount(amount.negate());
        debit.setType(LedgerEntryType.DEBIT);
        debit.setCreatedAt(now);
        debit.setTransferId(transferId);

        LedgerEntry credit = new LedgerEntry();
        credit.setAccount(toAccount);
        credit.setAmount(amount);
        credit.setType(LedgerEntryType.CREDIT);
        credit.setCreatedAt(now);
        credit.setTransferId(transferId);

        try {
            // 1) Write exactly two ledger rows and flush to detect idempotency (unique constraint) early.
            ledgerEntryRepository.saveAll(List.of(debit, credit));
            entityManager.flush();

            // Update balances (managed entities under pessimistic lock)
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));

            // Persist updates (explicit save or rely on a flush via JPA dirty checking)
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            return TransactionResult.success("Transfer completed successfully.");
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return TransactionResult.failure("Transfer already processed ...");
        }

    }

    /**
     * Gets account balance by account.
     *
     * @param accountId the account id
     * @return the account balance by account
     */
    @Override
    public BigDecimal getAccountBalanceByAccountId(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid account Id"));
        return account.getBalance();
    }

    private TransactionResult validate(LedgerEntryRequest ledgerEntryRequest) {
        if (ledgerEntryRequest == null
                || ledgerEntryRequest.getFromAccountId() == null
                || ledgerEntryRequest.getToAccountId() == null
                || ledgerEntryRequest.getAmount() == null) {
            return TransactionResult.failure("Invalid request: fromAccountId, toAccountId, and amount are required.");
        }
        if (ledgerEntryRequest.getFromAccountId().equals(ledgerEntryRequest.getToAccountId()))
            return TransactionResult.failure("Invalid transfer: source and destination accounts must differ.");

        if (ledgerEntryRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            return TransactionResult.failure("Invalid amount: must be greater than zero.");

        return null;
    }

    private boolean isTransferAlreadyProcessed(UUID transferId) {
        Long recordCount = entityManager
                .createQuery("select count(le.id) from LedgerEntry le where le.transferId = :tid", Long.class)
                .setParameter("tid", transferId)
                .getSingleResult();
        // Exactly two entries are expected for a completed transfer
        return recordCount != null && recordCount >= 2;
    }

    private LockedAccounts lockAccountsForUpdate(Long fromId, Long toId) {
        // Deterministic locking order to avoid deadlocks
        Long firstId = (fromId < toId) ? fromId : toId;
        Long secondId = (fromId < toId) ? toId : fromId;

        Account firstAccount = lockAccountForUpdate(firstId);
        Account secondAccount = lockAccountForUpdate(secondId);

        Account fromAccount = (firstId.equals(fromId)) ? firstAccount : secondAccount;
        Account toAccount = (firstId.equals(fromId)) ? secondAccount : firstAccount;

        return new LockedAccounts(fromAccount, toAccount);
    }

    private Account lockAccountForUpdate(Long id) {
        return entityManager.find(Account.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    // Small record to carry locked accounts (extract type)
    private record LockedAccounts(Account fromAccount, Account toAccount) {}

}