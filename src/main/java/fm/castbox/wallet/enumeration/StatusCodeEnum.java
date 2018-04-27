package fm.castbox.wallet.enumeration;

public class StatusCodeEnum {

  public static final int SUCCESS = 0;

  // General Errors Starts with 10, could be 10xxx;
  public static final int UNSUPPORTED_SYMBOL = 10001;
  public static final int INVALID_USER_ID = 10002;
  public static final int INVALID_SIGN = 10003;

  // Transfer Related Errors Starts with 20, could be 20xxx;
  public static final int REPEAT_TRANSFER_REQ = 20001;
  public static final int ADDRESS_OR_USER_ID_MISSING = 20002;
  public static final int ADDRESS_AND_USER_ID_BOTH_PRESENT = 20003;
  public static final int INSUFFICIENT_BALANCE = 20004;
  public static final int TRANSFER_AMOUNT_NEGATIVE = 20005;
  public static final int TRANSFER_AMOUNT_LESS_THAN_FEE = 20006;

}
