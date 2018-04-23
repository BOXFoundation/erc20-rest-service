package fm.castbox.wallet.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class EstFeeQDto {
  @NotBlank(message = "Symbol cannot be blank")
  private String symbol;

  @NotBlank(message = "Amount cannot be blank")
  @Pattern(regexp = "[0-9]+(.[0-9]+)?", message = "Invalid amount")
  private String amount;
}

