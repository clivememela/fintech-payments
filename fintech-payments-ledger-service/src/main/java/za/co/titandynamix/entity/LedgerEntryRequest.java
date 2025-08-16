package za.co.titandynamix.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ledger_entry_request")
public class LedgerEntryRequest {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "http_status")
    private Short httpStatus;

    @Column(name = "ledger_entry_request_status")
    private Short ledgerEntryRequestStatus;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "target_account_id")
    private UUID targetAccountId;

    @Size(max = 255)
    @Column(name = "info_message")
    private String infoMessage;

}