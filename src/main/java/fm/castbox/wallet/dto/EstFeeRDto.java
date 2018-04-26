package fm.castbox.wallet.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "status", "message" })
public class EstFeeRDto {
  private final int status;
  private final String message;

  private String feeEst;
  private String ethFeeEst;
  private Long timestamp;

  public EstFeeRDto(int status, String message){
    this.status = status;
    this.message = message;
  }
}

