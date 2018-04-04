package fm.castbox.wallet.dto;

import javax.validation.constraints.NotBlank;
import lombok.Value;
import org.hibernate.validator.constraints.Length;

@Value
public class AddressDto {

  @NotBlank
  @Length(min = 42, max = 42)
  private final String address;

}
