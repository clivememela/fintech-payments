package za.co.titandynamix.service;

import za.co.titandynamix.dto.LedgerEntryRequest;
import za.co.titandynamix.entity.LedgerEntry;

import java.util.List;
import java.util.UUID;

/**
 * The interface Transfer service.
 */
public interface TransferService {
    /**
     * Create a double entry transaction and return its transferId.
     *
     * @param ledgerEntryRequest the ledger entry request
     * @return the created transferId
     */
    UUID createDoubleEntryTransaction(LedgerEntryRequest ledgerEntryRequest);

    /**
     * Add a single credit transaction and return its transferId.
     *
     * @param ledgerEntryRequest the ledger entry request
     * @return the created transferId
     */
    UUID addTransaction(LedgerEntryRequest ledgerEntryRequest);

    /**
     * Gets all transactions.
     *
     * @return the all transactions
     */
    List<LedgerEntry> getAllTransactions();

    /**
     * Gets account ledger entries by account id.
     *
     * @param accountId the account id
     * @return the account ledger entries by account id
     */
    List<LedgerEntry> getAccountLedgerEntriesByAccountId(Long accountId);
}