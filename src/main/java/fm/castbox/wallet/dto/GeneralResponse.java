package fm.castbox.wallet.dto;


import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.ObjectError;

@Value
public class GeneralResponse {

  private final String message;
  private final String error;

  public GeneralResponse(final String message) {
    this.message = message;
    this.error = "OK";
  }

  public GeneralResponse(final String message, final String error) {
    this.message = message;
    this.error = error;
  }

  public GeneralResponse(List<ObjectError> allErrors, String error) {
    this.error = error;
    this.message = allErrors.stream()
        .map(DefaultMessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(","));
  }

}
