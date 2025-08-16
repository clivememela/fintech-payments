package za.co.titandynamix.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ledger entry: allow exactly one DEBIT and one CREDIT per transferId (idempotency).
 */
@Getter
@Setter
@Entity
@Table(
        name = "ledger_entry",
        uniqueConstraints = {
                // Prevent duplicate DEBITs or CREDITs for the same transferId; allow one of each.
                @UniqueConstraint(name = "uk_ledger_transfer_type", columnNames = {"transfer_id", "type"})
        },
        indexes = {
                @Index(name = "idx_ledger_transfer_id", columnList = "transfer_id"),
                @Index(name = "idx_ledger_account_id", columnList = "account_id")
        }
)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Shared idempotency key for both rows of a transfer (DEBIT and CREDIT)
    @Column(name = "transfer_id", nullable = false, updatable = false)
    private UUID transferId;

    /**
     * The account associated with the ledger entry transaction.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private LedgerEntryType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}