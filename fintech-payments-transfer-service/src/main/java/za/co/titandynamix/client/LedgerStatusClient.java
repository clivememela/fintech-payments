package za.co.titandynamix.client;

import java.util.UUID;

/**
 * Client interface for fetching transfer status from the Ledger Service.
 */
public interface LedgerStatusClient {
    
    /**
     * Response record for transfer status from Ledger Service.
     */
    record TransferStatusResponse(UUID transferId, String status, String message) {}
    
    /**
     * Fetch transfer status from the Ledger Service.
     * 
     * @param transferId the transfer ID to query
     * @return TransferStatusResponse with status information
     * @throws IllegalArgumentException if transfer not found or invalid ID
     */
    TransferStatusResponse getTransferStatus(UUID transferId);
}