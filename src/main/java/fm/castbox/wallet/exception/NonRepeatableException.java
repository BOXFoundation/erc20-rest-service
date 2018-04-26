package fm.castbox.wallet.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class NonRepeatableException extends IllegalStateException {

  private int status;

  public NonRepeatableException(String exName, String message) {
    super("NonRepeatable Request " + exName + ", " + message);
    this.status = HttpStatus.ACCEPTED.value();
  }

  public NonRepeatableException(int status, String exName, String message) {
    super("NonRepeatable Request " + exName + ", " + message);
    this.status = status;
  }
}
