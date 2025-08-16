package za.co.titandynamix.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.titandynamix.client.ResilientLedgerTransferClient;
import za.co.titandynamix.dto.TransferRequest;
import za.co.titandynamix.entity.Transfer;
import za.co.titandynamix.service.IdempotencyService;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Processes individual transfers with idempotency support.
 * Coordinates between Transfer Service idempotency and Ledger Service execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferProcessor {

    private final ResilientLedgerTransferClient resilientLedgerTransferClient;
    private final IdempotencyService idempotencyService;

    /**
     * Processes a single transfer with comprehensive idempotency support.
     * 
     * Idempotency is handled at two levels:
     * 1. Transfer Service level - prevents duplicate API calls
     * 2. Ledger Service level - prevents duplicate database operations
     * 
     * @param request the transfer request
     * @param idempotencyKeyOrNull optional idempotency key for duplicate prevention
     * @return Transfer object with status and details
     */
    public Transfer processSingle(TransferRequest request, String idempotencyKeyOrNull) {
        System.out.println("=== DEBUG: TransferProcessor.processSingle() ENTRY ===");
        System.out.println("DEBUG: Request: " + request);
        System.out.println("DEBUG: IdempotencyKey: " + idempotencyKeyOrNull);
        
        validate(request);
        System.out.println("DEBUG: Validation passed");

        // Check Transfer Service level idempotency first
        if (idempotencyKeyOrNull != null && !idempotencyKeyOrNull.trim().isEmpty()) {
            System.out.println("DEBUG: Checking idempotency cache");
            Optional<Transfer> cachedTransfer = idempotencyService.getCachedTransfer(idempotencyKeyOrNull);
            if (cachedTransfer.isPresent()) {
                System.out.println("DEBUG: Found cached transfer: " + cachedTransfer.get());
                log.debug("Returning cached transfer for idempotency key: {}", idempotencyKeyOrNull);
                return cachedTransfer.get();
            }
            System.out.println("DEBUG: No cached transfer found");
        }

        // Process new transfer via Resilient Ledger Service
        System.out.println("DEBUG: Processing new transfer via ResilientLedgerTransferClient");
        log.debug("Processing new transfer for accounts {} -> {}", 
                request.getFromAccountId(), request.getToAccountId());
        
        Transfer transfer = resilientLedgerTransferClient.createAndProcessTransfer(request, idempotencyKeyOrNull);
        System.out.println("DEBUG: ResilientLedgerTransferClient result: " + transfer);
        
        // Store result for future idempotency checks
        if (idempotencyKeyOrNull != null && !idempotencyKeyOrNull.trim().isEmpty()) {
            System.out.println("DEBUG: Storing transfer in idempotency cache");
            idempotencyService.storeTransfer(idempotencyKeyOrNull, transfer);
        }

        System.out.println("=== DEBUG: TransferProcessor.processSingle() EXIT ===");
        return transfer;
    }

    private void validate(TransferRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }
        if (req.getFromAccountId() == null || req.getToAccountId() == null) {
            throw new IllegalArgumentException("fromAccountId and toAccountId are required");
        }
        if (req.getFromAccountId().equals(req.getToAccountId())) {
            throw new IllegalArgumentException("fromAccountId and toAccountId must differ");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }
}