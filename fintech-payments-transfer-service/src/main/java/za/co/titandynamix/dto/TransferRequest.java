package za.co.titandynamix.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link za.co.titandynamix.entity.Transfer}
 */
@Value
@Schema(description = "Transfer request containing source account, destination account, and amount")
public class TransferRequest implements Serializable {
    
    @NotNull
    @Schema(description = "Source account ID to debit funds from", 
            example = "123", 
            required = true)
    Long fromAccountId;
    
    @NotNull 
    @Schema(description = "Destination account ID to credit funds to", 
            example = "456", 
            required = true)
    Long toAccountId;
    
    @NotNull 
    @Positive
    @Schema(description = "Transfer amount (must be positive)", 
            example = "100.50", 
            required = true,
            minimum = "0.01")
    BigDecimal amount;
}