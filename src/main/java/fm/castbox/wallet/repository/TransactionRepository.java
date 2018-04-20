package fm.castbox.wallet.repository;

import fm.castbox.wallet.domain.Transaction;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TransactionRepository extends PagingAndSortingRepository<Transaction, Long> {
    boolean existsByTxId(String txId);

    @Query("SELECT t FROM Transaction t WHERE t.fromAddress = ?1 or t.toAddress = ?1")
    List<Transaction> findByAddress(String address, Pageable pageable);
}