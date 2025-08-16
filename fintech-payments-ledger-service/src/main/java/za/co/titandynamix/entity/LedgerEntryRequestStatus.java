package za.co.titandynamix.entity;

import za.co.titandynamix.dto.LedgerEntryRequest;

/**
 * Enumeration representing the status of a {@link LedgerEntryRequest}.
 * <p>
 * This enumeration defines the possible states of a transfer request, indicating whether the request
 * is in progress or has been completed.
 * </p>
 */
public enum LedgerEntryRequestStatus {
    /**
     * In progress ledger entry request status.
     */
    IN_PROGRESS,
    /**
     * Completed ledger entry request status.
     */
    COMPLETED
}
