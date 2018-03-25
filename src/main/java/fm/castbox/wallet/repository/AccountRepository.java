package fm.castbox.wallet.repository;

import fm.castbox.wallet.domain.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountRepository extends CrudRepository<Account, Long> {
    List<Account> findByAddress(String address);
}