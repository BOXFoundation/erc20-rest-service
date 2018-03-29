package fm.castbox.wallet.repository;

import fm.castbox.wallet.domain.Account;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, String> {

  Optional<Account> findByEthAddress(String address);

  Optional<Account> findByUserId(String userId);
}
