package fm.castbox.wallet.repository;

import fm.castbox.wallet.domain.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountRepository extends CrudRepository<Account, String> {
    List<Account> findByAddress(String address);
    List<Account> findByUserId(String userId);
}