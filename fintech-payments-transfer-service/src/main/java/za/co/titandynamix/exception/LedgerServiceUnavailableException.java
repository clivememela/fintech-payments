package za.co.titandynamix.exception;

/**
 * Exception thrown when the Ledger Service is unavailable or degraded.
 * Used to indicate circuit breaker activation or service failures.
 */
public class LedgerServiceUnavailableException extends RuntimeException {
    
    public LedgerServiceUnavailableException(String message) {
        super(message);
    }
    
    public LedgerServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}