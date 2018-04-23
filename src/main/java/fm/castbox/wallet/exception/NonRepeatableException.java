package fm.castbox.wallet.exception;

public class NonRepeatableException extends IllegalStateException {

  public NonRepeatableException(String exName, String message) {
    super("NonRepeatable Request " + exName + ", " + message);
  }
}
