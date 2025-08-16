package za.co.titandynamix.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "banking_ledger_transaction")
public class BankingLedgerTransaction {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "amount", precision = 38, scale = 2)
    private BigDecimal amount;

    @Size(max = 255)
    @Column(name = "description")
    private String description;

    @Column(name = "\"timestamp\"")
    private Instant timestamp;

    @Size(max = 255)
    @Column(name = "type")
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

}