package za.co.titandynamix.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker configuration for resilience between Transfer Service and Ledger Service.
 * 
 * Implements fault tolerance patterns to prevent cascading failures when the Ledger Service
 * is degraded or unavailable. Provides structured logging for monitoring circuit breaker
 * state changes and failed calls.
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {

    public static final String LEDGER_SERVICE_CIRCUIT_BREAKER = "ledgerService";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreaker ledgerServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                // Circuit breaker opens after 5 consecutive failures
                .failureRateThreshold(60.0f)
                .minimumNumberOfCalls(5)
                
                // Wait 30 seconds before transitioning to half-open
                .waitDurationInOpenState(Duration.ofSeconds(30))
                
                // Allow 3 calls in half-open state to test recovery
                .permittedNumberOfCallsInHalfOpenState(3)
                
                // Sliding window of 10 calls for failure rate calculation
                .slidingWindowSize(10)
                .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                
                // Slow call threshold - calls taking longer than 5 seconds are considered failures
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                
                // Record specific exceptions as failures
                .recordExceptions(
                    RuntimeException.class,
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class
                )
                
                // Don't record business logic exceptions as circuit breaker failures
                .ignoreExceptions(IllegalArgumentException.class)
                
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker(LEDGER_SERVICE_CIRCUIT_BREAKER, config);
        
        // Add event listeners for structured logging
        addEventListeners(circuitBreaker);
        
        return circuitBreaker;
    }

    private void addEventListeners(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("Circuit breaker state transition: {} -> {} for service: {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState(),
                            LEDGER_SERVICE_CIRCUIT_BREAKER);
                    
                    // Structured logging for monitoring systems
                    log.info("circuit_breaker_state_change service={} from_state={} to_state={} timestamp={}",
                            LEDGER_SERVICE_CIRCUIT_BREAKER,
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState(),
                            event.getCreationTime());
                });

        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> {
                    log.warn("Circuit breaker call not permitted for service: {} - circuit is OPEN",
                            LEDGER_SERVICE_CIRCUIT_BREAKER);
                    
                    log.info("circuit_breaker_call_rejected service={} state={} timestamp={}",
                            LEDGER_SERVICE_CIRCUIT_BREAKER,
                            circuitBreaker.getState(),
                            event.getCreationTime());
                });

        circuitBreaker.getEventPublisher()
                .onFailureRateExceeded(event -> {
                    log.error("Circuit breaker failure rate exceeded: {}% for service: {}",
                            event.getFailureRate(),
                            LEDGER_SERVICE_CIRCUIT_BREAKER);
                    
                    log.info("circuit_breaker_failure_rate_exceeded service={} failure_rate={} timestamp={}",
                            LEDGER_SERVICE_CIRCUIT_BREAKER,
                            event.getFailureRate(),
                            event.getCreationTime());
                });

        circuitBreaker.getEventPublisher()
                .onSlowCallRateExceeded(event -> {
                    log.warn("Circuit breaker slow call rate exceeded: {}% for service: {}",
                            event.getSlowCallRate(),
                            LEDGER_SERVICE_CIRCUIT_BREAKER);
                    
                    log.info("circuit_breaker_slow_call_rate_exceeded service={} slow_call_rate={} timestamp={}",
                            LEDGER_SERVICE_CIRCUIT_BREAKER,
                            event.getSlowCallRate(),
                            event.getCreationTime());
                });

        circuitBreaker.getEventPublisher()
                .onSuccess(event -> {
                    log.debug("Circuit breaker successful call for service: {} - duration: {}ms",
                            LEDGER_SERVICE_CIRCUIT_BREAKER,
                            event.getElapsedDuration().toMillis());
                });

        circuitBreaker.getEventPublisher()
                .onError(event -> {
                    log.error("Circuit breaker failed call for service: {} - duration: {}ms, error: {}",
                            LEDGER_SERVICE_CIRCUIT_BREAKER,
                            event.getElapsedDuration().toMillis(),
                            event.getThrowable().getMessage());
                    
                    log.info("circuit_breaker_call_failed service={} duration_ms={} error_type={} error_message={} timestamp={}",
                            LEDGER_SERVICE_CIRCUIT_BREAKER,
                            event.getElapsedDuration().toMillis(),
                            event.getThrowable().getClass().getSimpleName(),
                            event.getThrowable().getMessage(),
                            event.getCreationTime());
                });
    }
}