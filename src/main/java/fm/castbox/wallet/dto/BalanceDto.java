package fm.castbox.wallet.dto;

import javax.validation.constraints.NotBlank;
import lombok.Value;
import org.hibernate.validator.constraints.Length;

@Value
public class BalanceDto {
  @NotBlank
  @Length(min = 2, max = 8)
  private String symbol;

  // balance in terms of tokens
  private String tokenAmount;

  // balance in terms of dollars
  private String usdAmount;
}