package za.co.titandynamix.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for {@link za.co.titandynamix.entity.Account}
 */
@Getter
@Setter
public class CreateAccountRequest {
    private String accountName;
    private BigDecimal balance;
    private UUID transferId;
}