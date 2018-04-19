package fm.castbox.wallet.service;


import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.dto.EstFeeQDto;
import fm.castbox.wallet.dto.EstFeeRDto;

import java.util.Optional;

public interface EthService {

  Optional<String> createAddress(String userId);

  Optional<String> changeAddress(String userId);

  BalanceDto balanceOf(String userId);

  EstFeeRDto estimateTransferFee(EstFeeQDto estFeeQDto);
}
