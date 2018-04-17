package fm.castbox.wallet.domain;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
  @Id
  // TODO: only applies to on-chain tx
  private String txId;

  private String fromUserId;
  // Note: for withdraw, this is sender's ethAddress. However, it is our main account ethAddress on chain as viewed in a block explorer
  private String fromAddress;

  private String toUserId;
  private String toAddress;

  @Min(value = 0L, message = "Amount cannot be negative")
  private long amount;

  @NotNull
  @Column(nullable = false)
  private Timestamp transactedAt;
}
