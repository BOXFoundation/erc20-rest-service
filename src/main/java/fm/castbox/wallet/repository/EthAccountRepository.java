package fm.castbox.wallet.repository;

import fm.castbox.wallet.domain.EthAccount;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface EthAccountRepository extends CrudRepository<EthAccount, String> {

  Optional<EthAccount> findByAddress(String address);

  boolean existsByUserId(String userId);

  Optional<EthAccount> findByUserId(String userId);

  EthAccount save(EthAccount ethAccount);
}
