package za.co.titandynamix.dto;

import java.util.UUID;

/**
 * Response DTO for transfer status queries in the ledger service.
 */
public record TransferStatusResponse(
        UUID transferId,
        String status,   // SUCCEEDED | PENDING | PARTIAL
        String message
) {}