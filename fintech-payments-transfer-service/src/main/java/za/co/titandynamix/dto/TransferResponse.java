package za.co.titandynamix.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import za.co.titandynamix.entity.TransferStatus;

import java.util.UUID;

@Schema(description = "Transfer response containing transfer ID, status, and optional failure reason")
public record TransferResponse(
        @Schema(description = "Unique transfer identifier", 
                example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,
        
        @Schema(description = "Current transfer status", 
                example = "SUCCEEDED",
                allowableValues = {"PENDING", "SUCCEEDED", "FAILED", "ERROR"})
        TransferStatus status,
        
        @Schema(description = "Failure reason (only present when status is FAILED or ERROR)", 
                example = "Insufficient funds in source account",
                nullable = true)
        String failureReason
) {}
