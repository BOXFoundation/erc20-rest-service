package fm.castbox.wallet.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EstFeeQDto {
  @NotBlank(message = "Symbol cannot be blank")
  private String symbol;

  @NotBlank(message = "Amount cannot be blank")
  private String amount;
}

