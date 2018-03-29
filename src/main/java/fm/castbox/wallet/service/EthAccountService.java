package fm.castbox.wallet.service;


import java.util.Optional;

public interface EthAccountService {

  Optional<String> createAddress(String userId);

  Optional<String> changeAddress(String userId);
}
