package fm.castbox.wallet.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

  @Id
  @Column(columnDefinition = "CHAR(32)")
  private String userId;  // userId from castbox

  @Column(unique = true, nullable = false, columnDefinition = "CHAR(42)")
  private String ethAddress;

  private long ethBalance;
}
