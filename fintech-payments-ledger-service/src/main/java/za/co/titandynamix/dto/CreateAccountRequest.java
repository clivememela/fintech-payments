package za.co.titandynamix.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for {@link za.co.titandynamix.entity.Account}
 */
@Getter
@Setter
@Schema(description = "Request to create a new account with initial balance")
public class CreateAccountRequest {
    
    @Schema(description = "Account name or identifier", 
            example = "ACC-001", 
            required = true)
    @NotNull
    private String accountName;
    
    @Schema(description = "Initial account balance (must be non-negative)", 
            example = "1000.00", 
            required = true,
            minimum = "0.00")
    @NotNull
    @PositiveOrZero
    private BigDecimal balance;
    
    @Schema(description = "Optional transfer ID for tracking", 
            example = "123e4567-e89b-12d3-a456-426614174000",
            nullable = true)
    private UUID transferId;
}