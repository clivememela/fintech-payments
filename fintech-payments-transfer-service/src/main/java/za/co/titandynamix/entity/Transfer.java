package za.co.titandynamix.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transfer entity - now a simple POJO for data transfer.
 * Actual persistence is handled by the Ledger Service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {
    private UUID id;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private TransferStatus status;
    private String idempotencyKey;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}