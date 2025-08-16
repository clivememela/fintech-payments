package za.co.titandynamix.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import za.co.titandynamix.dto.LedgerEntryRequest;
import za.co.titandynamix.dto.TransactionResult;

/**
 * Wrapper service that provides retry logic for optimistic locking failures.
 * This ensures that concurrent transfers don't fail due to version conflicts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetryableLedgerService {
    
    private final LedgerService ledgerService;
    
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 50;
    
    /**
     * Execute a double-entry transfer with retry logic for optimistic lock failures.
     * 
     * @param request the transfer request
     * @return TransactionResult with success/failure status
     */
    public TransactionResult createDoubleEntryTransactionWithRetry(LedgerEntryRequest request) {
        int attempt = 0;
        
        while (attempt < MAX_RETRIES) {
            try {
                return ledgerService.createDoubleEntryTransaction(request);
                
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                log.warn("Optimistic lock failure on attempt {} for transfer {}: {}", 
                        attempt, request.getTransferId(), e.getMessage());
                
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries exceeded for transfer {}", request.getTransferId());
                    return TransactionResult.failure("Transfer failed due to high concurrency. Please retry.");
                }
                
                // Brief delay before retry to reduce contention
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return TransactionResult.failure("Transfer interrupted during retry");
                }
            }
        }
        
        return TransactionResult.failure("Unexpected error in retry logic");
    }
}