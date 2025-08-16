package za.co.titandynamix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Transaction result.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResult {
    private boolean success;
    private String message;

    /**
     * Success transaction result.
     *
     * @param message the message
     * @return the transaction result
     */
    public static TransactionResult success(String message) {
        return new TransactionResult(true, message);
    }

    /**
     * Failure transaction result.
     *
     * @param message the message
     * @return the transaction result
     */
    public static TransactionResult failure(String message) {
        return new TransactionResult(false, message);
    }
}
