package fm.castbox.wallet.domain;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "eth_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EthAccount {

  @Id
  @Column(columnDefinition = "CHAR(32)")
  @Length(min = 32, max = 32)
  private String userId;  // userId from castbox

  // prefixed with "0x"
  @NotBlank
  @Length(min = 42, max = 42)
  @Column(nullable = false, columnDefinition = "CHAR(42)")
  private String address;

  @NotBlank
  @Length(min = 64, max = 64)
  // TODO: add "unique = true"
  @Column(nullable = false, columnDefinition = "CHAR(64)")
  private String privateKey;

  private long balance;

  @NotNull
  @Column(nullable = false)
  private Timestamp createdAt;

  @NotNull
  @Column(nullable = false)
  private Timestamp updatedAt;
}
