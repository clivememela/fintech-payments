package za.co.titandynamix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link za.co.titandynamix.entity.Account}
 */
@Data
@AllArgsConstructor
public class AccountResponse implements Serializable {
    private Long id;
    private String name;
    private BigDecimal balance;
    private Long version;
}