package fm.castbox.wallet.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Value;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

@Value
public class BalanceDto {

  @NotBlank
  @Length(min = 2, max = 8)
  private String symbol;

  @NotBlank
  @Length(min = 42, max = 42)
  private final String address;

  @Range(min = 0)
  @Min(value = 0L, message = "The balance must be positive")
  private double balance;
}
