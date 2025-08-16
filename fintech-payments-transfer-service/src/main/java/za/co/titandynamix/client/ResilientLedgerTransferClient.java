package za.co.titandynamix.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.titandynamix.config.CircuitBreakerConfig;
import za.co.titandynamix.dto.TransferRequest;
import za.co.titandynamix.entity.Transfer;
import za.co.titandynamix.entity.TransferStatus;
import za.co.titandynamix.exception.LedgerServiceUnavailableException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Resilient wrapper for LedgerTransferClient that implements circuit breaker pattern
 * for fault tolerance and cascading failure prevention.
 * 
 * Features:
 * - Circuit breaker protection against Ledger Service failures
 * - Structured logging for monitoring and alerting
 * - Graceful degradation with fallback responses
 * - Automatic recovery detection
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientLedgerTransferClient {

    private final LedgerTransferClient ledgerTransferClient;
    private final CircuitBreaker ledgerServiceCircuitBreaker;

    /**
     * Creates and processes a transfer with circuit breaker protection.
     * 
     * @param request the transfer request
     * @param idempotencyKey optional idempotency key
     * @return Transfer object with status
     * @throws LedgerServiceUnavailableException when circuit is open or service unavailable
     */
    public Transfer createAndProcessTransfer(TransferRequest request, String idempotencyKey) {
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        
        log.info("ledger_service_call_attempt operation_id={} from_account={} to_account={} amount={} idempotency_key={}",
                operationId,
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                idempotencyKey != null ? idempotencyKey : "none");

        Supplier<Transfer> transferSupplier = CircuitBreaker.decorateSupplier(
                ledgerServiceCircuitBreaker,
                () -> {
                    try {
                        long startTime = System.currentTimeMillis();
                        Transfer result = ledgerTransferClient.createAndProcessTransfer(request, idempotencyKey);
                        long duration = System.currentTimeMillis() - startTime;
                        
                        log.info("ledger_service_call_success operation_id={} transfer_id={} status={} duration_ms={}",
                                operationId,
                                result.getId(),
                                result.getStatus(),
                                duration);
                        
                        return result;
                    } catch (Exception e) {
                        log.error("ledger_service_call_error operation_id={} error_type={} error_message={}",
                                operationId,
                                e.getClass().getSimpleName(),
                                e.getMessage());
                        throw e;
                    }
                }
        );

        try {
            return transferSupplier.get();
        } catch (CallNotPermittedException e) {
            log.error("ledger_service_circuit_open operation_id={} circuit_state={} message={}",
                    operationId,
                    ledgerServiceCircuitBreaker.getState(),
                    "Circuit breaker is OPEN - Ledger Service calls not permitted");
            
            // Return a fallback response indicating service unavailability
            return createFallbackTransfer(request, "Ledger Service temporarily unavailable - circuit breaker is OPEN");
            
        } catch (Exception e) {
            log.error("ledger_service_call_failed operation_id={} error_type={} error_message={} circuit_state={}",
                    operationId,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    ledgerServiceCircuitBreaker.getState());
            
            // For non-circuit breaker exceptions, still provide fallback
            return createFallbackTransfer(request, "Transfer failed due to service error: " + e.getMessage());
        }
    }

    /**
     * Gets circuit breaker metrics for monitoring.
     */
    public CircuitBreakerMetrics getCircuitBreakerMetrics() {
        var metrics = ledgerServiceCircuitBreaker.getMetrics();
        var state = ledgerServiceCircuitBreaker.getState();
        
        return CircuitBreakerMetrics.builder()
                .serviceName(CircuitBreakerConfig.LEDGER_SERVICE_CIRCUIT_BREAKER)
                .state(state.toString())
                .failureRate(metrics.getFailureRate())
                .slowCallRate(metrics.getSlowCallRate())
                .numberOfSuccessfulCalls(metrics.getNumberOfSuccessfulCalls())
                .numberOfFailedCalls(metrics.getNumberOfFailedCalls())
                .numberOfSlowCalls(metrics.getNumberOfSlowCalls())
                .numberOfNotPermittedCalls(metrics.getNumberOfNotPermittedCalls())
                .build();
    }

    /**
     * Creates a fallback transfer response when the Ledger Service is unavailable.
     */
    private Transfer createFallbackTransfer(TransferRequest request, String reason) {
        log.warn("ledger_service_fallback_response from_account={} to_account={} amount={} reason={}",
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                reason);
        
        return Transfer.builder()
                .id(UUID.randomUUID())
                .fromAccountId(request.fromAccountId())
                .toAccountId(request.toAccountId())
                .amount(request.amount())
                .status(TransferStatus.FAILED)
                .failureReason(reason)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Data class for circuit breaker metrics.
     */
    @lombok.Builder
    @lombok.Data
    public static class CircuitBreakerMetrics {
        private String serviceName;
        private String state;
        private float failureRate;
        private float slowCallRate;
        private long numberOfSuccessfulCalls;
        private long numberOfFailedCalls;
        private long numberOfSlowCalls;
        private long numberOfNotPermittedCalls;
    }
}