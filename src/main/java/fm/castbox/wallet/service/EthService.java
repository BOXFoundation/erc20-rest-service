package fm.castbox.wallet.service;


import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.dto.EstFeeResponse;

import java.util.Optional;

public interface EthService {

  Optional<String> createAddress(String userId);

  Optional<String> changeAddress(String userId);

  BalanceDto balanceOf(String userId);

  EstFeeResponse estimateTransferFee(String symbol, String amount);
}
