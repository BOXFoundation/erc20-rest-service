package fm.castbox.wallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * TransferRDto wrapper.
 */
@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "status", "message" })
public class TransferRDto<T> {

    private final int status;
    private final String message;

    private String txId;
    private String state;
    private T event;

    public TransferRDto(String txId) {
      this(txId, null);
    }

    public TransferRDto(String txId, T event) {
      this.status = 0;
      this.message = "OK";
      this.txId = txId;
      this.event = event;
    }
}
