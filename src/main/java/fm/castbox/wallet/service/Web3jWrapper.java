package fm.castbox.wallet.service;

import java.math.BigInteger;

import lombok.experimental.Delegate;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

import fm.castbox.wallet.properties.NodeProperties;

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
              Transaction.createEthCallTransaction(null, nodeProperties.getFromAddress(), ""))
              .sendAsync().get();
      return ethEstimateGas.getAmountUsed();
  }
}
