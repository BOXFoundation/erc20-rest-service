package fm.castbox.wallet.dto;

import javax.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class AddressDto {
  @NotBlank
  private final String address;
}
