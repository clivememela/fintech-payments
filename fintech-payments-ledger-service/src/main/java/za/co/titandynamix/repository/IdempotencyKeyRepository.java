package za.co.titandynamix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.titandynamix.entity.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    IdempotencyKey findByKey(String key);
}