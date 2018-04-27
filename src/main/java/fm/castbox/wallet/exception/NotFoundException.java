package fm.castbox.wallet.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class NotFoundException extends RuntimeException {

  private int status;

  public NotFoundException(String exName, String message) {
    super(exName + " " + message);
    this.status = HttpStatus.NOT_FOUND.value();
  }

  public NotFoundException(int status, String exName, String message) {
    super(exName + " " + message);
    this.status = status;
  }
}
