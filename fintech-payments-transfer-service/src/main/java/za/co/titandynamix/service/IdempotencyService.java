package za.co.titandynamix.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.titandynamix.entity.Transfer;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing idempotency in transfer operations.
 * Uses in-memory storage with TTL for duplicate request prevention.
 * 
 * In production, consider using Redis or database for persistence across restarts.
 */
@Service
@Slf4j
public class IdempotencyService {
    
    private final ConcurrentHashMap<String, IdempotencyRecord> store = new ConcurrentHashMap<>();
    private static final int TTL_HOURS = 24;
    
    /**
     * Check if an idempotency key has been used and return cached result if valid.
     */
    public Optional<Transfer> getCachedTransfer(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }
        
        IdempotencyRecord record = store.get(idempotencyKey);
        if (record == null || record.isExpired()) {
            if (record != null) {
                store.remove(idempotencyKey);
                log.debug("Removed expired idempotency record for key: {}", idempotencyKey);
            }
            return Optional.empty();
        }
        
        log.debug("Found valid cached transfer for idempotency key: {}", idempotencyKey);
        return Optional.of(record.transfer);
    }
    
    /**
     * Store transfer result for future idempotency checks.
     */
    public void storeTransfer(String idempotencyKey, Transfer transfer) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }
        
        IdempotencyRecord record = new IdempotencyRecord(
                transfer, 
                LocalDateTime.now().plusHours(TTL_HOURS)
        );
        
        store.put(idempotencyKey, record);
        log.debug("Stored transfer for idempotency key: {} with ID: {}", idempotencyKey, transfer.getId());
    }
    
    /**
     * Clean up expired records every hour.
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredRecords() {
        int removedCount = 0;
        var iterator = store.entrySet().iterator();
        
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.info("Cleaned up {} expired idempotency records", removedCount);
        }
    }
    
    /**
     * Get current store size for monitoring.
     */
    public int getStoreSize() {
        return store.size();
    }
    
    /**
     * Internal record for storing transfer with expiration.
     */
    private static class IdempotencyRecord {
        final Transfer transfer;
        final LocalDateTime expiresAt;
        
        IdempotencyRecord(Transfer transfer, LocalDateTime expiresAt) {
            this.transfer = transfer;
            this.expiresAt = expiresAt;
        }
        
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}