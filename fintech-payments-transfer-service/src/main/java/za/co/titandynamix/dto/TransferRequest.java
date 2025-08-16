package za.co.titandynamix.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import za.co.titandynamix.entity.Transfer;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link Transfer}
 */
@Schema(description = "Transfer request containing source account, destination account, and amount")
public record TransferRequest(@Schema(description = "Source account ID to debit funds from",
        example = "123",
        required = true) @NotNull Long fromAccountId, @Schema(description = "Destination account ID to credit funds to",
        example = "456",
        required = true) @NotNull Long toAccountId, @Schema(description = "Transfer amount (must be positive)",
        example = "100.50",
        required = true,
        minimum = "0.01") @NotNull @Positive BigDecimal amount) implements Serializable {

}