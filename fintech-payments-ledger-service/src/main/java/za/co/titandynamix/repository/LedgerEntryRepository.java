package za.co.titandynamix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.titandynamix.entity.Account;
import za.co.titandynamix.entity.LedgerEntry;
import za.co.titandynamix.entity.LedgerEntryType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Ledger entry repository.
 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    /**
     * Find ledger entry by account list.
     *
     * @param account the account
     * @return the list
     */
    List<LedgerEntry> findLedgerEntryByAccount(Account account);

    /**
     * Find by transfer id and type optional.
     *
     * @param transferId the transfer id
     * @param type       the type
     * @return the optional
     */
    Optional<LedgerEntry> findByTransferIdAndType(UUID transferId, LedgerEntryType type);

    /**
     * Find all by transfer id list.
     *
     * @param transferId the transfer id
     * @return the list
     */
    List<LedgerEntry> findAllByTransferId(UUID transferId);
}