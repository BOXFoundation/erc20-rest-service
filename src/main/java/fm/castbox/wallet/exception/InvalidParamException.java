package fm.castbox.wallet.exception;

public class InvalidParamException extends IllegalArgumentException {

  public InvalidParamException(String param, String message) {
    super("Invalid param " + param + ", " + message);
  }
}
