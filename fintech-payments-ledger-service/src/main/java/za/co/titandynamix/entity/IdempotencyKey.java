package za.co.titandynamix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "key", nullable = false)
    private String key;

    private String response;
    private LocalDateTime expirationDate;

}