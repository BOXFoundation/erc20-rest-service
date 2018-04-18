package fm.castbox.wallet.service;

import fm.castbox.wallet.domain.EthAccount;
import lombok.experimental.Delegate;
import org.springframework.context.annotation.Scope;
import org.web3j.protocol.core.methods.request.Transaction;
import fm.castbox.wallet.dto.TransactionResponse;
import fm.castbox.wallet.exception.UserNotExistException;
import fm.castbox.wallet.generated.HumanStandardToken;
import fm.castbox.wallet.properties.NodeProperties;
import fm.castbox.wallet.properties.WalletProperties;
import fm.castbox.wallet.repository.EthAccountRepository;
import fm.castbox.wallet.repository.TransactionRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import rx.Subscription;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

/**
 * Web3jService Wrapper.
 */
@Service
@Scope("singleton")
public class Web3jWrapper {

  @Delegate
  private final Web3j web3j;

  private final NodeProperties nodeProperties;

  @Autowired
  public Web3jWrapper(NodeProperties nodeProperties){
    this.nodeProperties = nodeProperties;
    this.web3j = Web3j.build(new HttpService(nodeProperties.getNodeEndpoint()));
  }

  public BigInteger estimateTransferGas() throws Exception {
      EthEstimateGas ethEstimateGas = ethEstimateGas(
              Transaction.createEthCallTransaction(nodeProperties.getFromAddress(), null, ""))
              .sendAsync().get();
      return ethEstimateGas.getAmountUsed();
  }
}
