package fm.castbox.wallet.domain;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


@Entity
@Table(name = "address_history")
@Data
public class AddressHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Length(min = 32, max = 32)
  @Column(nullable = false, columnDefinition = "CHAR(32)")
  private final String userId;

  @NotBlank
  @Length(min = 2, max = 16)
  @Column(nullable = false, length = 16)
  private final String coin;

  @NotBlank
  @Length(min = 8, max = 64)
  @Column(unique = true, nullable = false, length = 64)
  private final String address;

  @NotBlank
  @Length(min = 8, max = 128)
  @Column(unique = true, nullable = false, length = 128)
  private final String privateKey;

  @NotNull
  @Column(nullable = false)
  private final Timestamp deprecatedAt;
}
