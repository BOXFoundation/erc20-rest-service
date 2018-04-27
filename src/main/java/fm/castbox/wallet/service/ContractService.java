package fm.castbox.wallet.service;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import fm.castbox.wallet.domain.EthAccount;
import fm.castbox.wallet.domain.Transaction;
import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.dto.TransferRDto;
import fm.castbox.wallet.enumeration.StatusCodeEnum;
import fm.castbox.wallet.enumeration.TransactionEnum;
import fm.castbox.wallet.exception.NonRepeatableException;
import fm.castbox.wallet.exception.UserNotExistException;
import fm.castbox.wallet.generated.HumanStandardToken;
import fm.castbox.wallet.properties.NodeProperties;
import fm.castbox.wallet.properties.WalletProperties;
import fm.castbox.wallet.repository.EthAccountRepository;
import fm.castbox.wallet.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;
import org.web3j.utils.Numeric;
import rx.Subscription;

/**
 * Our smart contract service.
 */
@Service
@Slf4j
public class ContractService {

  private final NodeProperties nodeProperties;

  private final WalletProperties walletProperties;

  private final Web3j web3j;

  private final Credentials masterAccountCred;

  private HashMap<String /* contract address */, Subscription /* contract event subscription */> contractSubscriptions = new HashMap<>();

  private HashMap<String /* token symbol */, ContractInstance /* token contract details */> tokenSymbolContracts = new HashMap<>();

  private EthAccountRepository ethAccountRepository;

  private TransactionRepository transactionRepository;

  @Autowired
  public ContractService(NodeProperties nodeProperties,
      WalletProperties walletProperties, EthAccountRepository ethAccountRepository,
      TransactionRepository transactionRepository) throws Exception {
    this.nodeProperties = nodeProperties;
    this.walletProperties = walletProperties;
    this.ethAccountRepository = ethAccountRepository;
    this.transactionRepository = transactionRepository;

    web3j = Web3j.build(new HttpService(nodeProperties.getNodeEndpoint()));

    masterAccountCred = Credentials.create(walletProperties.getPrivateKey());
    log.info("Master account address " + masterAccountCred.getAddress());

    subscribeToEthTransferEvents();
  }

  private void receiveTokens(String tokenSymbol, String txId, String from, String to,
      BigInteger amount) {
    System.out.println(
        " rx token  : " + tokenSymbol + "\n"
            + " txId    : " + txId + "\n"
            + " from    : " + from + "\n"
            + " to      : " + to + "\n"
            + " amount   : " + amount);
    Optional<EthAccount> accountOptional = ethAccountRepository.findByAddress(to);
    if (!accountOptional.isPresent()) {
      return;
    }

    EthAccount toEthAccount = accountOptional.get();
    switch (tokenSymbol) {
      case "BOX":
        toEthAccount.setBoxBalance(toEthAccount.getBoxBalance().add(amount));
        break;
      case "ETH":
        toEthAccount.setEthBalance(toEthAccount.getEthBalance().add(amount));
        break;
      default:
        System.out.println("Unsupported token: " + tokenSymbol);
        return;
    }
    ethAccountRepository.save(toEthAccount);
    // Only append tx not saved before. Otherwise previous send tx will be overwritten with main account as from address
    if (!transactionRepository.existsByTxId(txId)) {
      String amountStr;
      try {
        amountStr = min2BasicUnit(tokenSymbol, amount).toString();
      } catch (Exception e) {
        System.out.println(e.toString());
        return;
      }
      Timestamp now = new Timestamp(System.currentTimeMillis());
      transactionRepository.save(new Transaction(txId, tokenSymbol, null , from,
          null, to, amountStr,  null, TransactionEnum.EXTERNAL_TYPE, TransactionEnum.DONE_STATE, now, now));
    }
  }

  // is tx contract related: creation or call
  boolean isContractTx(org.web3j.protocol.core.methods.response.Transaction tx) throws RuntimeException {
    if (tx.getTo() == null) {
      // contract creation tx
      return true;
    }

    try {
      EthGetCode ethGetCode = web3j.ethGetCode(tx.getTo(), DefaultBlockParameterName.LATEST).send();
      // externally owned address has no code and getCode() returns "0x"
      return ethGetCode.getCode().length() > 2;
    } catch (Exception e) {
      // convert to unchecked RuntimeException since subscribe() complains checked exception
      throw new RuntimeException(e.toString());
    }
  }

  private void subscribeToEthTransferEvents() throws Exception {
    web3j.transactionObservable().subscribe(
        txEvent -> {
          // only monitor eth transfer tx between externally owned accounts, not contract creation/call
          if (isContractTx(txEvent)) {
            System.out.println("Contract creation or call transaction, ignore");
            return;
          }
          receiveTokens("ETH", txEvent.getHash(), txEvent.getFrom(), txEvent.getTo(),
              txEvent.getValue());
        }, Throwable::printStackTrace);
  }

  public void subscribeToContractTransferEvents(String contractAddress) throws Exception {
    if (contractSubscriptions.containsKey(contractAddress)) {
      System.out.println("Already subscribed to contract " + contractAddress);
      return;
    }

    HumanStandardToken humanStandardToken = load(contractAddress);
    String tokenSymbol;
    try {
      tokenSymbol = symbol(contractAddress);
    } catch (Exception e) {
      throw e;
    }

    Subscription sub = web3j.blockObservable(false).subscribe(block -> {
      BigInteger blockNum = block.getBlock().getNumber();
      DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(blockNum);
      System.out.println("Received block: " + blockNum);
      humanStandardToken.transferEventObservable(blockParameter, blockParameter)
          .subscribe(txEvent -> {
            receiveTokens(tokenSymbol, txEvent._transactionHash, txEvent._from, txEvent._to,
                txEvent._value);
          });
    }, Throwable::printStackTrace);
    contractSubscriptions.put(contractAddress, sub);
    System.out.println("Subscribed to contract " + contractAddress);
  }

  public void unsubscribeToContractTransferEvents(String contractAddress) {
    if (!contractSubscriptions.containsKey(contractAddress)) {
      System.out.println("Trying to unsubscribe from non-subscribed contract " + contractAddress);
      return;
    }
    Subscription sub = contractSubscriptions.remove(contractAddress);
    sub.unsubscribe();
    System.out.println("Unsubscribed from contract " + contractAddress);
  }

  public List<String> listContractSubscriptions() {
    return new ArrayList<>(contractSubscriptions.keySet());
  }

  public NodeProperties getConfig() {
    return nodeProperties;
  }

  public String deploy(
      List<String> privateFor, BigInteger initialAmount, String tokenName, BigInteger decimalUnits,
      String tokenSymbol) throws Exception {
    try {
      HumanStandardToken humanStandardToken = HumanStandardToken.deploy(
          web3j, masterAccountCred, GAS_PRICE, GAS_LIMIT,
          initialAmount, tokenName, decimalUnits,
          tokenSymbol).send();
      String contractAddress = humanStandardToken.getContractAddress();
      ContractInstance contractInstance = new ContractInstance(contractAddress, initialAmount,
          tokenName, decimalUnits, tokenSymbol);
      // TODO: symbol duplicate detection
      tokenSymbolContracts.put(tokenSymbol, contractInstance);
      subscribeToContractTransferEvents(contractAddress);
      return contractAddress;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String getUserAddress(String userId) throws Exception {
    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
    if (accountOptional.isPresent()) {
      return accountOptional.get().getAddress();
    }
    // create a new account for user
    ECKeyPair ecKeyPair = Keys.createEcKeyPair();
    // prepend w/ "0x"
    String address = Numeric.prependHexPrefix(Keys.getAddress(ecKeyPair));
    // convert from BigInteger
    String privateKey = ecKeyPair.getPrivateKey().toString(16);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // initial balance 0
    ethAccountRepository.save(new EthAccount(userId, address, privateKey, "0", "0", now, now));
    return address;
  }

  public List<BalanceDto> getUserBalances(String userId) throws Exception {
    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(userId, "BOX");
    }
    EthAccount account = accountOptional.get();
    // TODO: fill in dollar amount
    return Arrays.asList(new BalanceDto("ETH", min2BasicUnit("ETH", account.getEthBalance()).toString(), "0"),
        new BalanceDto("BOX", min2BasicUnit("BOX", account.getBoxBalance()).toString(), "0"));
  }

  public String name(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return humanStandardToken.name().send();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public TransferRDto<ApprovalEventResponse> approve(
      List<String> privateFor, String contractAddress, String spender, BigInteger value)
      throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .approve(spender, value).send();
      return processApprovalEventResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public long totalSupply(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return extractLongValue(humanStandardToken.totalSupply().send());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public long decimals(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return extractLongValue(humanStandardToken.decimals().send());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String version(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return humanStandardToken.version().send();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public long balanceOf(String contractAddress, String ownerAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return extractLongValue(humanStandardToken.balanceOf(ownerAddress).send());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String symbol(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return humanStandardToken.symbol().send();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public TransferRDto<TransferEventResponse> transfer(
      List<String> privateFor, String contractAddress, String to, BigInteger value)
      throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .transfer(to, value).send();
      return processTransferEventsResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public TransferRDto<TransferEventResponse> transferEth(String to, BigInteger value)
      throws Exception {
    BigInteger nonce = getNonce(nodeProperties.getFromAddress());
    RawTransaction rawTx = RawTransaction.createEtherTransaction(
        nonce, GAS_PRICE, GAS_LIMIT, to, value);

    byte[] signedMessage = TransactionEncoder.signMessage(rawTx, masterAccountCred);
    String hexValue = Numeric.toHexString(signedMessage);

    EthSendTransaction ethSendTransaction =
        web3j.ethSendRawTransaction(hexValue).send();
    Response.Error ethError = ethSendTransaction.getError();
    if ( Optional.ofNullable(ethError).isPresent() ) {
      if (ethError.getCode() == -32000) { // known transaction error
        throw new NonRepeatableException(StatusCodeEnum.REPEAT_TRANSFER_REQ, "transfer", ethError.getMessage());
      } else {
        throw new RuntimeException("Web3j Error " + ethError.getCode() + ", " + ethError.getMessage());
      }
    }
    return new TransferRDto<>(ethSendTransaction.getTransactionHash());
  }

  public TransferRDto<ApprovalEventResponse> approveAndCall(
      List<String> privateFor, String contractAddress, String spender, BigInteger value,
      String extraData) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .approveAndCall(
              spender, value,
              extraData.getBytes())
          .send();
      return processApprovalEventResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public long allowance(String contractAddress, String ownerAddress, String spenderAddress)
      throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return extractLongValue(humanStandardToken.allowance(
          ownerAddress, spenderAddress)
          .send());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Transaction> getTransactions(String userId, Pageable pageable) {
    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(userId, "ETH");
    }
    return transactionRepository.findByAddress(accountOptional.get().getAddress(), pageable);
  }

  public Optional<Transaction> getTransaction(Long id) {
    return transactionRepository.findById(id);
  }

  private HumanStandardToken load(String contractAddress) {
    return HumanStandardToken.load(
        contractAddress, web3j, masterAccountCred, GAS_PRICE, GAS_LIMIT);
  }

  private long extractLongValue(BigInteger value) {
    return value.longValueExact();
  }

  private TransferRDto<ApprovalEventResponse>
  processApprovalEventResponse(
      HumanStandardToken humanStandardToken,
      TransactionReceipt transactionReceipt) {

    return processEventResponse(
        humanStandardToken.getApprovalEvents(transactionReceipt),
        transactionReceipt,
        ApprovalEventResponse::new);
  }

  private TransferRDto<TransferEventResponse>
  processTransferEventsResponse(
      HumanStandardToken humanStandardToken,
      TransactionReceipt transactionReceipt) {

    return processEventResponse(
        humanStandardToken.getTransferEvents(transactionReceipt),
        transactionReceipt,
        TransferEventResponse::new);
  }

  private <T, R> TransferRDto<R> processEventResponse(
      List<T> eventResponses, TransactionReceipt transactionReceipt, Function<T, R> map) {
    if (!eventResponses.isEmpty()) {
      return new TransferRDto<>(
          transactionReceipt.getTransactionHash(),
          map.apply(eventResponses.get(0)));
    } else {
      return new TransferRDto<>(
          transactionReceipt.getTransactionHash());
    }
  }

  BigInteger getNonce(String address) throws Exception {
    return web3j.ethGetTransactionCount(
        address, DefaultBlockParameterName.LATEST).send().getTransactionCount();
  }

  public String tokenSymbol2ContractAddr(String tokenSymbol) {
    ContractInstance contractInstance = tokenSymbolContracts.get(tokenSymbol);
    if (contractInstance == null) {
      throw new RuntimeException("Unknown contract for token " + tokenSymbol);
    }
    return contractInstance.getAddress();
  }

  // convert from basic unit to its minimal unit, e.g., eth -> wei
  public BigInteger basic2MinUnit(String tokenSymbol, String amount) throws Exception {
    if (tokenSymbol.equals("ETH")) {
      return Convert.toWei(amount, Unit.ETHER).toBigIntegerExact();
    }

    ContractInstance contractInstance = tokenSymbolContracts.get(tokenSymbol);
    if (contractInstance == null) {
      throw new RuntimeException("Unknown contract for token " + tokenSymbol);
    }
    int decimalUnits = contractInstance.getDecimalUnits().intValueExact();
    BigDecimal bdAmount = new BigDecimal(amount);
    return bdAmount.multiply(BigDecimal.TEN.pow(decimalUnits)).toBigIntegerExact();
  }

  // convert from minimal unit to its basic unit, e.g., wei -> eth
  public BigDecimal min2BasicUnit(String tokenSymbol, BigInteger amount) throws Exception {
    BigDecimal bdAmount = new BigDecimal(amount);
    if (tokenSymbol.equals("ETH")) {
      return Convert.fromWei(bdAmount, Unit.ETHER);
    }

    ContractInstance contractInstance = tokenSymbolContracts.get(tokenSymbol);
    if (contractInstance == null) {
      throw new RuntimeException("Unknown contract for token " + tokenSymbol);
    }
    int decimalUnits = contractInstance.getDecimalUnits().intValueExact();
    return bdAmount.divide(BigDecimal.TEN.pow(decimalUnits));
  }

  @Getter
  @Setter
  public static class TransferEventResponse {

    private String from;
    private String to;
    private long value;

    TransferEventResponse(
        HumanStandardToken.TransferEventResponse transferEventResponse) {
      this.from = transferEventResponse._from;
      this.to = transferEventResponse._to;
      this.value = transferEventResponse._value.longValueExact();
    }
  }

  @Getter
  @Setter
  public static class ApprovalEventResponse {

    private String owner;
    private String spender;
    private long value;

    ApprovalEventResponse(
        HumanStandardToken.ApprovalEventResponse approvalEventResponse) {
      this.owner = approvalEventResponse._owner;
      this.spender = approvalEventResponse._spender;
      this.value = approvalEventResponse._value.longValueExact();
    }
  }

  @Data
  private class ContractInstance {
    private final String address;
    // specification
    private final BigInteger initialAmount;
    private final String tokenName;
    private final BigInteger decimalUnits;
    private final String tokenSymbol;
  }
}
