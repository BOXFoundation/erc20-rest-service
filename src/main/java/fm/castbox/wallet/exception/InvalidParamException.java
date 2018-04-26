package fm.castbox.wallet.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class InvalidParamException extends IllegalArgumentException {

  private int status;

  public InvalidParamException(String param, String message) {
    super("Invalid param " + param + ", " + message);
    this.status = HttpStatus.BAD_REQUEST.value();
  }

  public InvalidParamException(int status, String param, String message) {
    super("Invalid param " + param + ", " + message);
    this.status = status;
  }
}
