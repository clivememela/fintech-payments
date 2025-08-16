package za.co.titandynamix.client;

import za.co.titandynamix.entity.Transfer;
import za.co.titandynamix.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerTransferClient {
    record Result(boolean success, String message) {}
    
    /**
     * Legacy method - performs transfer operation
     */
    Result performTransfer(UUID transferId, Long fromAccountId, Long toAccountId, BigDecimal amount);
    
    /**
     * Creates and processes a transfer with idempotency support.
     * The Ledger Service will handle both transfer creation and execution.
     * 
     * @param request the transfer request
     * @param idempotencyKey optional idempotency key for duplicate prevention
     * @return Transfer object with status and details
     */
    Transfer createAndProcessTransfer(TransferRequest request, String idempotencyKey);
}