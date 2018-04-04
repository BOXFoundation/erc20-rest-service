package fm.castbox.wallet.service;


import fm.castbox.wallet.dto.BalanceDto;
import java.util.Optional;

public interface EthService {

  Optional<String> createAddress(String userId);

  Optional<String> changeAddress(String userId);

  BalanceDto balanceOf(String userId);
}
