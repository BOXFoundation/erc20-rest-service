package fm.castbox.wallet.domain;

import java.math.BigInteger;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import fm.castbox.wallet.enumeration.StatusCodeEnum;
import fm.castbox.wallet.exception.InvalidParamException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

// account on Ethereum, including eth and ERC-20 tokens
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

  @NotBlank
  @Length(min = 42, max = 42)
  @Column(unique = true, nullable = false, columnDefinition = "CHAR(42)")
  private String address;

  @NotBlank
  @Length(min = 64, max = 64)
  // TODO: add unique check
  @Column(nullable = false, columnDefinition = "CHAR(64)")
  private String privateKey;

  // String to accommodate BigInteger
  @NotBlank
  private String ethBalance;

  // String to accommodate BigInteger
  @NotBlank
  private String boxBalance;

  @NotNull
  @Column(nullable = false)
  private Timestamp createdAt;

  @NotNull
  @Column(nullable = false)
  private Timestamp updatedAt;

  public BigInteger getEthBalance() {
    return new BigInteger(ethBalance);
  }

  public BigInteger getBoxBalance() {
    return new BigInteger(boxBalance);
  }

  public void setEthBalance(BigInteger ethBalance) {
    this.ethBalance = ethBalance.toString();
  }

  public void setBoxBalance(BigInteger boxBalance) {
    this.boxBalance = boxBalance.toString();
  }

  public BigInteger getBalanceOf(String tokenSymbol) throws InvalidParamException {
    switch (tokenSymbol.toUpperCase()) {
      case "BOX":
        return getBoxBalance();
      case "ETH":
        return getEthBalance();
      default:
        throw new InvalidParamException(StatusCodeEnum.UNSUPPORTED_SYMBOL, "symbol", "unsupported token: " + tokenSymbol);
    }
  }

  public void setBalanceOf(String tokenSymbol, BigInteger balance) {
    switch (tokenSymbol.toUpperCase()) {
      case "BOX":
        setBoxBalance(balance);
        break;
      case "ETH":
        setEthBalance(balance);
        break;
      default:
        throw new InvalidParamException(StatusCodeEnum.UNSUPPORTED_SYMBOL, "symbol", "unsupported token: " + tokenSymbol);
    }
  }
}
