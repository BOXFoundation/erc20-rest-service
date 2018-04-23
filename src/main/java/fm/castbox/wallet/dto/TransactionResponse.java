package fm.castbox.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * TransactionResponse wrapper.
 */
@Getter
@Setter
@AllArgsConstructor
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse<T> {

    private final int status;
    private final String message;
    private String txId;
    private T event;

    public TransactionResponse(String txId) {
      this(txId, null);
    }

    public TransactionResponse(String txId, T event) {
      this.status = 0;
      this.message = "OK";
      this.txId = txId;
      this.event = event;
    }
}
