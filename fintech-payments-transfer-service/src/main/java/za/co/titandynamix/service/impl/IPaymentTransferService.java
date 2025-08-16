package za.co.titandynamix.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.titandynamix.client.LedgerStatusClient;
import za.co.titandynamix.dto.TransferRequest;
import za.co.titandynamix.entity.Transfer;
import za.co.titandynamix.entity.TransferStatus;
import za.co.titandynamix.service.PaymentTransferService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IPaymentTransferService implements PaymentTransferService {

    private final TransferProcessor transferProcessor;
    private final LedgerStatusClient ledgerStatusClient;

    @Override
    public Transfer getTransferStatus(TransferRequest request, String idempotencyKey) {
        System.out.println("=== DEBUG: IPaymentTransferService.getTransferStatus() ENTRY ===");
        System.out.println("DEBUG: Request: " + request);
        System.out.println("DEBUG: IdempotencyKey: " + idempotencyKey);
        
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            System.out.println("DEBUG: Throwing IllegalArgumentException - missing idempotency key");
            throw new IllegalArgumentException("Missing required Idempotency-Key header");
        }

        System.out.println("DEBUG: Calling transferProcessor.processSingle()");
        Transfer result = transferProcessor.processSingle(request, idempotencyKey);
        System.out.println("DEBUG: TransferProcessor result: " + result);
        System.out.println("=== DEBUG: IPaymentTransferService.getTransferStatus() EXIT ===");
        
        return result;
    }

    @Override
    public Transfer getTransferStatus(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }
        
        // Fetch transfer status from Ledger Service
        LedgerStatusClient.TransferStatusResponse ledgerResponse = ledgerStatusClient.getTransferStatus(id);
        
        // Map Ledger Service response to Transfer entity
        return mapLedgerResponseToTransfer(id, ledgerResponse);
    }

    @Override
    public List<Transfer> createTransfersBatch(List<TransferRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        if (requests.size() > 100) {
            throw new IllegalArgumentException("Batch size must be <= 100");
        }

        log.info("Processing batch of {} transfers using CompletableFuture", requests.size());
        
        // Use CompletableFuture for better concurrency control
        // Thread pool sized based on available processors and batch size
        int threadPoolSize = Math.min(requests.size(), Runtime.getRuntime().availableProcessors() * 2);
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        try {
            // Create CompletableFuture for each transfer
            List<CompletableFuture<Transfer>> futures = new ArrayList<>();
            
            for (int i = 0; i < requests.size(); i++) {
                final int index = i;
                final TransferRequest request = requests.get(i);
                
                // Generate deterministic idempotency key for batch items
                String batchIdempotencyKey = generateBatchIdempotencyKey(index, request);
                
                CompletableFuture<Transfer> future = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return transferProcessor.processSingle(request, batchIdempotencyKey);
                        } catch (Exception e) {
                            log.error("Error processing transfer at index {}: {}", index, e.getMessage());
                            return Transfer.builder()
                                    .id(UUID.randomUUID())
                                    .status(TransferStatus.FAILED)
                                    .failureReason("Processing error: " + e.getMessage())
                                    .build();
                        }
                    }, executor);
                
                futures.add(future);
            }
            
            // Wait for all transfers to complete with timeout
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );
            
            try {
                allFutures.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("Batch processing timeout - some transfers may still be processing");
            } catch (ExecutionException e) {
                log.warn("Batch processing execution error", e);
            }
            
            // Collect results
            List<Transfer> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            
            for (CompletableFuture<Transfer> future : futures) {
                try {
                    Transfer transfer = future.get();
                    results.add(transfer);
                    
                    if (transfer.getStatus() == TransferStatus.SUCCEEDED) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Error retrieving transfer result", e);
                    results.add(Transfer.builder()
                            .id(UUID.randomUUID())
                            .status(TransferStatus.FAILED)
                            .failureReason("Result retrieval error")
                            .build());
                    failureCount++;
                }
            }
            
            log.info("Batch processing completed: {} successful, {} failed out of {} total", 
                    successCount, failureCount, requests.size());
            
            return results;
            
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Batch processing interrupted", ie);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Generate deterministic idempotency key for batch items.
     * This ensures that retrying the same batch produces the same results.
     */
    private String generateBatchIdempotencyKey(int index, TransferRequest request) {
        return String.format("batch_%d_%s_%s_%s", 
                index, 
                request.fromAccountId(),
                request.toAccountId(),
                request.amount().toString());
    }

    /**
     * Maps Ledger Service response to Transfer entity.
     * Since Transfer Service doesn't store transfer details, we create a minimal Transfer
     * object with the status information from the Ledger Service.
     */
    private Transfer mapLedgerResponseToTransfer(UUID transferId, LedgerStatusClient.TransferStatusResponse ledgerResponse) {
        TransferStatus status = mapLedgerStatusToTransferStatus(ledgerResponse.status());
        
        return Transfer.builder()
                .id(transferId)
                .status(status)
                .failureReason(status == TransferStatus.FAILED ? ledgerResponse.message() : null)
                .build();
    }

    /**
     * Maps Ledger Service status strings to TransferStatus enum.
     */
    private TransferStatus mapLedgerStatusToTransferStatus(String ledgerStatus) {
        return switch (ledgerStatus) {
            case "SUCCEEDED" -> TransferStatus.SUCCEEDED;
            case "NOT_FOUND", "PARTIAL", "ERROR", "INVALID", "UNKNOWN" -> TransferStatus.FAILED;
            default -> TransferStatus.PENDING;
        };
    }
}