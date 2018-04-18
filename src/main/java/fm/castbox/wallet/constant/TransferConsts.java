package fm.castbox.wallet.constant;

import lombok.Value;
import java.math.BigDecimal;

@Value
public class TransferConsts {
  public static final BigDecimal FIXED_ETH_BOX_RATE = new BigDecimal("100000");
}
