package fm.castbox.wallet.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

@Value
public class BalanceDto {

  @NotBlank
  @Length(min = 2, max = 8)
  private String symbol;

  // balance in terms of tokens
  @Min(value = 0L, message = "Balance cannot be negative")
  private double tokenAmount;

  // balance in terms of dollars
  @Min(value = 0L, message = "Balance cannot be negative")
  private double usdAmount;
}
