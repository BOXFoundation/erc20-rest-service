package fm.castbox.wallet.dto;

import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.math.BigInteger;

@Data
public class TransferQDto { // Query(Request) DTO
  @NotBlank(message = "Symbol cannot be blank")
  private String tokenSymbol;

  @NotBlank(message = "Amount cannot be blank")
  @Pattern(regexp = "[0-9]+(.[0-9]+)?", message = "Invalid amount")
  private String amount;

  @Digits(integer = 13, fraction = 0)
  private long timeStamp; // to prevent replay attack

  @NotBlank(message = "Sign cannot be blank")
  private String sign; // signature

  private String toAddress;
  private String toUserId;
  private String note;
}

