package fm.castbox.wallet.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstFeeResponse {
  private final int retCode;
  private final String retMsg;

  private String feeEst;
  private String ethFeeEst;
  private Long timestamp;

  public EstFeeResponse(int retCode, String retMsg){
    this.retCode = retCode;
    this.retMsg = retMsg;
  }
}

