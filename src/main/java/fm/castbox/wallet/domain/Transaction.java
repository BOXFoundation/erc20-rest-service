package fm.castbox.wallet.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

  @Id
  private String txId;
  // Note: for withdraw, this is sender's ethAddress. However, it is our main account ethAddress on chain as viewed in a block explorer
  private String fromAddress;
  private String toAddress;
  private long value;
}
