package fm.castbox.wallet.dto;


import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fm.castbox.wallet.enumeration.CommonEnum;
import fm.castbox.wallet.enumeration.StatusCodeEnum;
import lombok.Value;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.validation.ObjectError;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "_status", "_message" })
public class GeneralResponse<T> {

  private final int _status;
  private final String _message;

  private String error;
  private T payload;

  public GeneralResponse(int status, final String message, final String error) {
    this._status = status;
    this._message = message;
    this.error = error;
    payload = null;
  }

  public GeneralResponse(List<ObjectError> allErrors, String error, int status) {
    this._status = status;
    this.error = error;
    this.payload = null;
    this._message = allErrors.stream()
        .map(DefaultMessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(","));
  }

  public GeneralResponse(T payload){
    this._status = StatusCodeEnum.SUCCESS;
    this._message = CommonEnum.OK;
    this.payload = payload;
    this.error = null;
  }

  public GeneralResponse(int status, String message, T payload) {
    this._status = status;
    this._message = message;
    this.payload = payload;
    this.error = null;
  }

  @JsonProperty(value = "status")
  public int getStatus(){
    int status = _status;
    try {
      if (payload != null && PropertyUtils.isReadable(payload, "status")) {
        // use the status of payload
        status = (int) PropertyUtils.getProperty(payload, "status");
      }
    } catch (Exception e) {
      Assert.isTrue(false, "Shouldn't be here");
    }
    return status;
  }

  @JsonProperty(value = "message")
  public String getMessage(){
    String message = _message;
    try {
      if (payload != null && PropertyUtils.isReadable(payload,"message")) {
        // use the message of payload
        message = (String) PropertyUtils.getProperty(payload, "message");
      }
    } catch (Exception e) {
      Assert.isTrue(false, "Shouldn't be here");
    }
    return message;
  }
}
