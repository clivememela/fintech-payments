package za.co.titandynamix.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.titandynamix.client.ResilientLedgerTransferClient;

/**
 * REST Controller for monitoring resilience patterns and circuit breaker health.
 * Provides endpoints for operational visibility into service-to-service communication health.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/monitoring")
public class ResilienceMonitoringController {

    private final ResilientLedgerTransferClient resilientLedgerTransferClient;

    /**
     * Get circuit breaker metrics for the Ledger Service.
     * 
     * Provides operational visibility into:
     * - Circuit breaker state (CLOSED, OPEN, HALF_OPEN)
     * - Failure and success rates
     * - Call statistics
     * - Performance metrics
     * 
     * @return Circuit breaker metrics and health status
     */
    @GetMapping("/circuit-breaker/ledger-service")
    public ResponseEntity<ResilientLedgerTransferClient.CircuitBreakerMetrics> getLedgerServiceCircuitBreakerMetrics() {
        ResilientLedgerTransferClient.CircuitBreakerMetrics metrics = 
                resilientLedgerTransferClient.getCircuitBreakerMetrics();
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get overall resilience health status.
     * 
     * @return Health status summary
     */
    @GetMapping("/health")
    public ResponseEntity<ResilienceHealthStatus> getResilienceHealth() {
        ResilientLedgerTransferClient.CircuitBreakerMetrics metrics = 
                resilientLedgerTransferClient.getCircuitBreakerMetrics();
        
        ResilienceHealthStatus health = ResilienceHealthStatus.builder()
                .ledgerServiceAvailable(!"OPEN".equals(metrics.getState()))
                .ledgerServiceCircuitBreakerState(metrics.getState())
                .overallHealth(determineOverallHealth(metrics))
                .build();
        
        return ResponseEntity.ok(health);
    }

    private String determineOverallHealth(ResilientLedgerTransferClient.CircuitBreakerMetrics metrics) {
        if ("OPEN".equals(metrics.getState())) {
            return "DEGRADED";
        } else if ("HALF_OPEN".equals(metrics.getState())) {
            return "RECOVERING";
        } else if (metrics.getFailureRate() > 30.0f) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class ResilienceHealthStatus {
        private boolean ledgerServiceAvailable;
        private String ledgerServiceCircuitBreakerState;
        private String overallHealth;
    }
}