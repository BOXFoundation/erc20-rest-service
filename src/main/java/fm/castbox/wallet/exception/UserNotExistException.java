package fm.castbox.wallet.exception;

public class UserNotExistException extends IllegalArgumentException {

  public UserNotExistException(String userId) {
    super("userId " + userId + "does NOT exist");
  }
}
