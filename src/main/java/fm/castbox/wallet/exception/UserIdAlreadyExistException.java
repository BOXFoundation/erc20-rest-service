package fm.castbox.wallet.exception;

public class UserIdAlreadyExistException extends IllegalArgumentException {

  public UserIdAlreadyExistException(String userId, String coin) {
    super("userId " + userId + "already has " + coin + " address");
  }
}
