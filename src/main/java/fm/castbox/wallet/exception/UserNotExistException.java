package fm.castbox.wallet.exception;

public class UserNotExistException extends IllegalArgumentException {

  public UserNotExistException(String userId, String coin) {
    super("userId " + userId + "does NOT have " + coin + " address yet");
  }
}
