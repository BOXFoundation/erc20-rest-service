package fm.castbox.wallet.domain;

import com.fasterxml.jackson.annotation.JsonFilter;

import java.sql.Time;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;

// entities with generated Id can extend this so they can use @NoArgsConstructor & @AllArgsConstructor
@Entity
@Table(name = "transaction")
@Getter
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
abstract class BaseGeneratedId {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
// Generating equals/hashCode implementation but without a call to superclass
@EqualsAndHashCode(callSuper=false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction extends BaseGeneratedId {
  // TODO: only applies to on-chain tx
  private String txId;

  @Length(min = 2)
  @Column(nullable = false)
  private String tokenSymbol;

  private String fromUserId;
  // Note: for withdraw, this is sender's ethAddress. However, it is our main account ethAddress on chain as viewed in a block explorer
  private String fromAddress;

  private String toUserId;
  private String toAddress;

  @NotBlank
  @Column(nullable = false)
  private String amount;

  @Length(max = 255)
  private String note;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String state;

  @NotNull
  @Column(nullable = false)
  @CreationTimestamp
  private Timestamp createdAt;

  @NotNull
  @Column(nullable = false)
  @UpdateTimestamp
  private Timestamp updatedAt;
}
