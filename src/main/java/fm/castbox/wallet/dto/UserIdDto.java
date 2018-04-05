package fm.castbox.wallet.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Value;

@Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserIdDto {

  @NotBlank
  @Pattern(regexp = "[a-zA-Z0-9]{32}", message = "Invalid userId")
  private final String userId;
}
