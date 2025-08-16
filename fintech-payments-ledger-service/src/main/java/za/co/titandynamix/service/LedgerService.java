package za.co.titandynamix.service;

import org.springframework.transaction.annotation.Transactional;
import za.co.titandynamix.dto.CreateAccountRequest;
import za.co.titandynamix.dto.LedgerEntryRequest;
import za.co.titandynamix.dto.TransactionResult;
import za.co.titandynamix.entity.Account;

import java.math.BigDecimal;
import java.util.List;

/**
 * The interface Ledger service.
 */
public interface LedgerService {
    /**
     * Create account.
     *
     * @param createAccountRequest the creation account request
     * @return the account
     */
    @Transactional
    Account createAccount(String idempotencyKey,CreateAccountRequest createAccountRequest);

    /**
     * Gets all accounts.
     *
     * @return the all accounts
     */
    List<Account> getAllAccounts();


    /**
     * Gets account by account id.
     *
     * @param accountId the account id
     * @return the account by account id
     */
    Account getAccountByAccountId(Long accountId);

    /**
     * Apply a double-entry transfer (debit from one account, credit to another).
     * <p>
     * Contract:
     * - Input contains: transferId, fromAccountId, toAccountId, amount.
     * - Idempotent by transferId:
     * - If transferId has already been processed, return the original successful
     * TransactionResult without applying changes again.
     * - Enforce idempotency via a database-level unique constraint on transferId
     * (e.g., in the transfer aggregate or a dedicated table/column that records processed transfers).
     * - Atomic and consistent:
     * - Execute within a single transaction in the service implementation.
     * - Lock/update both accounts in a consistent order using optimistic versioning
     * or SELECT ... FOR UPDATE (pessimistic) to prevent race conditions.
     * - Reject operations that would produce a negative balance.
     * - Persist exactly two LedgerEntry rows (one DEBIT for fromAccountId, one CREDIT for toAccountId)
     * linked to the same transferId.
     * <p>
     * Note: The @Transactional boundary must be declared on the implementing class/method.
     *
     * @param transferRequest the transfer request (must include transferId)
     * @return TransactionResult indicating success or failure
     */
    TransactionResult createDoubleEntryTransaction(LedgerEntryRequest transferRequest);

    /**
     * Get current account balance.
     *
     * @param accountId the account id
     * @return the current balance
     */
    BigDecimal getAccountBalanceByAccountId(Long accountId);
}
