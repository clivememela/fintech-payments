package za.co.titandynamix.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.titandynamix.dto.LedgerEntryRequest;
import za.co.titandynamix.entity.Account;
import za.co.titandynamix.entity.LedgerEntry;
import za.co.titandynamix.entity.LedgerEntryType;
import za.co.titandynamix.repository.AccountRepository;
import za.co.titandynamix.repository.LedgerEntryRepository;
import za.co.titandynamix.service.LedgerService;
import za.co.titandynamix.service.TransferService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The type Transfer service.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ITransferService implements TransferService {
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    public UUID createDoubleEntryTransaction(LedgerEntryRequest ledgerEntryRequest) {
        Objects.requireNonNull(ledgerEntryRequest, "ledgerEntryRequest cannot be null");

        if (ledgerEntryRequest.getAmount() == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (ledgerEntryRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (ledgerEntryRequest.getFromAccountId() == null || ledgerEntryRequest.getToAccountId() == null) {
            throw new IllegalArgumentException("Both fromAccountId and toAccountId are required");
        }
        if (ledgerEntryRequest.getFromAccountId().equals(ledgerEntryRequest.getToAccountId())) {
            throw new IllegalArgumentException("fromAccountId and toAccountId cannot be the same");
        }

        Account debitAccount = accountRepository.findById(ledgerEntryRequest.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("The debit account ID is invalid"));

        Account creditAccount = accountRepository.findById(ledgerEntryRequest.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("The credit account ID is invalid"));

        UUID transferId = (ledgerEntryRequest.getTransferId() != null)
                ? ledgerEntryRequest.getTransferId()
                : UUID.randomUUID();

        LedgerEntry debitLedgerTransaction = new LedgerEntry();
        debitLedgerTransaction.setTransferId(transferId);
        debitLedgerTransaction.setAccount(debitAccount);
        debitLedgerTransaction.setAmount(ledgerEntryRequest.getAmount().negate());
        debitLedgerTransaction.setType(LedgerEntryType.DEBIT);

        LedgerEntry creditLedgerTransaction = new LedgerEntry();
        creditLedgerTransaction.setTransferId(transferId);
        creditLedgerTransaction.setAccount(creditAccount);
        creditLedgerTransaction.setAmount(ledgerEntryRequest.getAmount());
        creditLedgerTransaction.setType(LedgerEntryType.CREDIT);

        ledgerEntryRepository.saveAll(List.of(debitLedgerTransaction, creditLedgerTransaction));

        return transferId;
    }

    @Override
    public UUID addTransaction(LedgerEntryRequest ledgerEntryRequest) {
        Objects.requireNonNull(ledgerEntryRequest, "ledgerEntryRequest cannot be null");

        if (ledgerEntryRequest.getAmount() == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (ledgerEntryRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (ledgerEntryRequest.getToAccountId() == null) {
            throw new IllegalArgumentException("toAccountId is required");
        }

        Account creditAccount = accountRepository.findById(ledgerEntryRequest.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("The credit account ID is invalid"));

        UUID transferId = (ledgerEntryRequest.getTransferId() != null)
                ? ledgerEntryRequest.getTransferId()
                : UUID.randomUUID();

        LedgerEntry creditLedgerTransaction = new LedgerEntry();
        creditLedgerTransaction.setTransferId(transferId);
        creditLedgerTransaction.setAccount(creditAccount);
        creditLedgerTransaction.setAmount(ledgerEntryRequest.getAmount());
        creditLedgerTransaction.setType(LedgerEntryType.CREDIT);

        ledgerEntryRepository.save(creditLedgerTransaction);

        return transferId;
    }

    @Override
    public List<LedgerEntry> getAllTransactions() {
        return ledgerEntryRepository.findAll();
    }

    @Override
    public List<LedgerEntry> getAccountLedgerEntriesByAccountId(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId cannot be null");
        }
        Account account = ledgerService.getAccountByAccountId(accountId);
        return ledgerEntryRepository.findLedgerEntryByAccount(account);
    }
}