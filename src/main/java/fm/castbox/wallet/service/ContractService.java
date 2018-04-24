package fm.castbox.wallet.service;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import fm.castbox.wallet.domain.EthAccount;
import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.dto.TransferQDto;
import fm.castbox.wallet.exception.InvalidParamException;
import fm.castbox.wallet.exception.NonRepeatableException;
import fm.castbox.wallet.exception.UserNotExistException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import fm.castbox.wallet.domain.Transaction;
import fm.castbox.wallet.generated.HumanStandardToken;
import fm.castbox.wallet.properties.NodeProperties;
import fm.castbox.wallet.dto.TransactionResponse;
import fm.castbox.wallet.properties.WalletProperties;
import fm.castbox.wallet.repository.EthAccountRepository;
import fm.castbox.wallet.repository.TransactionRepository;
import fm.castbox.wallet.util.APISignUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.stereotype.Service;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;
import rx.Subscription;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

/**
 * Our smart contract service.
 */
@Service
@Slf4j
public class ContractService {

  // TODO: full node does not return private key, may fill in later
  private static final String DUMMY_PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  // override default timeout of 20 attempts every 1 second
  private static final int SLEEP_DURATION = 1000;
  private static final int ATTEMPTS = 60;

  private final Quorum quorum;

  private final NodeProperties nodeProperties;

  private final WalletProperties walletProperties;


  private final Admin admin;

  private final Web3j web3j;

  private HashMap<String /* contract address */, Subscription /* contract event subscription */> contractSubscriptions = new HashMap<>();

  private HashMap<String /* token symbol */, ContractInstance /* token contract details */> tokenSymbolContracts = new HashMap<>();

  private EthAccountRepository ethAccountRepository;

  private TransactionRepository transactionRepository;

  @Autowired
  public ContractService(Quorum quorum, NodeProperties nodeProperties,
      WalletProperties walletProperties, EthAccountRepository ethAccountRepository,
      TransactionRepository transactionRepository) throws Exception {
    this.quorum = quorum;
    this.nodeProperties = nodeProperties;
    this.walletProperties = walletProperties;
    this.ethAccountRepository = ethAccountRepository;
    this.transactionRepository = transactionRepository;

    admin = Admin.build(new HttpService(nodeProperties.getNodeEndpoint()));

    web3j = Web3j.build(new HttpService(nodeProperties.getNodeEndpoint()));

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
      transactionRepository.save(new Transaction(txId, tokenSymbol, "" /* fromUserId */, from,
          "" /* toUserId */, to, amountStr, now, ""));
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
    unlockAccount();

    try {
      TransactionManager transactionManager = new ClientTransactionManager(
          quorum, nodeProperties.getFromAddress(), privateFor, ATTEMPTS, SLEEP_DURATION);
      HumanStandardToken humanStandardToken = HumanStandardToken.deploy(
          quorum, transactionManager, GAS_PRICE, GAS_LIMIT,
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
    String address = admin.personalNewAccount(walletProperties.getPassphrase()).send()
        .getAccountId();
    // initial balance 0
    Timestamp now = new Timestamp(System.currentTimeMillis());
    ethAccountRepository.save(new EthAccount(userId, address, DUMMY_PRIVATE_KEY, "0", "0", now, now));
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

  public TransactionResponse<ApprovalEventResponse> approve(
      List<String> privateFor, String contractAddress, String spender, BigInteger value)
      throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress, privateFor);
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

  public TransactionResponse<TransferEventResponse> transferFromUser(
          List<String> privateFor, String fromUserId, TransferQDto transferQDto) throws Exception {
    if (!transferQDto.getTokenSymbol().equals("ETH") && !transferQDto.getTokenSymbol().equals("BOX")) {
      throw new InvalidParamException("tokenSymbol", "unsupported token: " + transferQDto.getTokenSymbol());
    }
    if (!Optional.ofNullable(transferQDto.getToUserId()).isPresent()
            && !Optional.ofNullable(transferQDto.getToAddress()).isPresent()) {
      throw new InvalidParamException("toUserId & toAddress", "cannot be both missing");
    }
    if (Optional.ofNullable(transferQDto.getToUserId()).isPresent()
            && Optional.ofNullable(transferQDto.getToAddress()).isPresent()) {
      throw new InvalidParamException("toUserId & toAddress", "cannot be both present");
    }

    // validate sign
    String toSignStr = APISignUtils.prepareStrToSign(transferQDto, "sign");
    boolean isSignValid = APISignUtils.verifySign(toSignStr, transferQDto.getSign());
    if (!isSignValid) {
      log.info("toSignStr is " + toSignStr);
//      throw new InvalidParamException("sign", "srcStr is " + toSignStr);
    }

    // look up toAddress from toUserId
    if (!transferQDto.getToUserId().isEmpty()) {
      Optional<EthAccount> toAddressOptional = ethAccountRepository.findByUserId(transferQDto.getToUserId());
      if (!toAddressOptional.isPresent()) {
        throw new UserNotExistException(transferQDto.getToUserId(), "ETH");
      }
      transferQDto.setToAddress(toAddressOptional.get().getAddress());
    }

    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(fromUserId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(fromUserId, "ETH");
    }

    EthAccount fromEthAccount = accountOptional.get();
    BigInteger value = basic2MinUnit(transferQDto.getTokenSymbol(), transferQDto.getAmount());
    BigInteger balance = transferQDto.getTokenSymbol().equals("ETH") ? fromEthAccount.getEthBalance() : fromEthAccount.getBoxBalance();
    if (balance.compareTo(value) < 0) {
      throw new InvalidParamException("amount", "Insufficient fund");
    }

    // transfer from main account, not fromEthAccount
    TransactionResponse<TransferEventResponse> txResponse;
    if (transferQDto.getTokenSymbol().equals("ETH")) {
      txResponse = transferEth(transferQDto.getToAddress(), value);
      fromEthAccount.setEthBalance(balance.subtract(value));
    } else {
      txResponse = transfer(privateFor, tokenSymbol2ContractAddr(transferQDto.getTokenSymbol()), transferQDto.getToAddress(), value);
      fromEthAccount.setBoxBalance(balance.subtract(value));
    }
    ethAccountRepository.save(fromEthAccount);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    transactionRepository.save(
        new Transaction(txResponse.getTxId(), transferQDto.getTokenSymbol(), fromUserId,
            fromEthAccount.getAddress(), transferQDto.getToUserId(), transferQDto.getToAddress(),
            min2BasicUnit(transferQDto.getTokenSymbol(), value).toString(), now, transferQDto.getNote()));
    return txResponse;
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

  public TransactionResponse<TransferEventResponse> transfer(
      List<String> privateFor, String contractAddress, String to, BigInteger value)
      throws Exception {
    unlockAccount();
    HumanStandardToken humanStandardToken = load(contractAddress, privateFor);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .transfer(to, value).send();
      return processTransferEventsResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public TransactionResponse<TransferEventResponse> transferEth(String to, BigInteger value)
      throws Exception {
    unlockAccount();

    BigInteger nonce = getNonce(nodeProperties.getFromAddress());
    org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction
        .createEtherTransaction(
            nodeProperties.getFromAddress(), nonce, GAS_PRICE, GAS_LIMIT, to, value);

    EthSendTransaction ethSendTransaction = web3j.ethSendTransaction(transaction).send();
    Response.Error ethError = ethSendTransaction.getError();
    if ( Optional.ofNullable(ethError).isPresent() ) {
      if (ethError.getCode() == -32000) { // known transaction error
        throw new NonRepeatableException("transfer", ethError.getMessage());
      } else {
        throw new RuntimeException("Web3j Error " + ethError.getCode() + ", " + ethError.getMessage());
      }
    }
    return new TransactionResponse<>(ethSendTransaction.getTransactionHash());
  }

  public TransactionResponse<ApprovalEventResponse> approveAndCall(
      List<String> privateFor, String contractAddress, String spender, BigInteger value,
      String extraData) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress, privateFor);
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

  public MappingJacksonValue getTransactions(String userId, String fields, Pageable pageable) {
    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(userId, "ETH");
    }
    List<Transaction> txs = transactionRepository.findByAddress(accountOptional.get().getAddress(), pageable);

    MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(txs);
    SimpleFilterProvider filters = new SimpleFilterProvider().setFailOnUnknownId(false);
    // return all fields when not specified
    if (fields != null) {
      String[] includedFields = fields.split(",");
      filters.addFilter("txFilter", SimpleBeanPropertyFilter.filterOutAllExcept(includedFields));
    }
    mappingJacksonValue.setFilters(filters);
    return mappingJacksonValue;
  }

  public MappingJacksonValue getTransaction(Long id) {
    Transaction tx = transactionRepository.findById(id).get();
    MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(tx);
    SimpleFilterProvider filters = new SimpleFilterProvider().setFailOnUnknownId(false);
    mappingJacksonValue.setFilters(filters);
    return mappingJacksonValue;
  }

  private HumanStandardToken load(String contractAddress, List<String> privateFor) {
    TransactionManager transactionManager = new ClientTransactionManager(
        quorum, nodeProperties.getFromAddress(), privateFor, ATTEMPTS, SLEEP_DURATION);
    return HumanStandardToken.load(
        contractAddress, quorum, transactionManager, GAS_PRICE, GAS_LIMIT);
  }

  private HumanStandardToken load(String contractAddress) {
    TransactionManager transactionManager = new ClientTransactionManager(
        quorum, nodeProperties.getFromAddress(), Collections.emptyList(), ATTEMPTS, SLEEP_DURATION);
    return HumanStandardToken.load(
        contractAddress, quorum, transactionManager, GAS_PRICE, GAS_LIMIT);
  }

  private long extractLongValue(BigInteger value) {
    return value.longValueExact();
  }

  private TransactionResponse<ApprovalEventResponse>
  processApprovalEventResponse(
      HumanStandardToken humanStandardToken,
      TransactionReceipt transactionReceipt) {

    return processEventResponse(
        humanStandardToken.getApprovalEvents(transactionReceipt),
        transactionReceipt,
        ApprovalEventResponse::new);
  }

  private TransactionResponse<TransferEventResponse>
  processTransferEventsResponse(
      HumanStandardToken humanStandardToken,
      TransactionReceipt transactionReceipt) {

    return processEventResponse(
        humanStandardToken.getTransferEvents(transactionReceipt),
        transactionReceipt,
        TransferEventResponse::new);
  }

  private <T, R> TransactionResponse<R> processEventResponse(
      List<T> eventResponses, TransactionReceipt transactionReceipt, Function<T, R> map) {
    if (!eventResponses.isEmpty()) {
      return new TransactionResponse<>(
          transactionReceipt.getTransactionHash(),
          map.apply(eventResponses.get(0)));
    } else {
      return new TransactionResponse<>(
          transactionReceipt.getTransactionHash());
    }
  }

  private void unlockAccount() throws Exception {
    PersonalUnlockAccount personalUnlockAccount = admin.
        personalUnlockAccount(nodeProperties.getFromAddress(), walletProperties.getPassphrase())
        .send();
    if (null == personalUnlockAccount.accountUnlocked()) {
      throw new Exception("Unlocking account failed");
    }
  }

  BigInteger getNonce(String address) throws Exception {
    return web3j.ethGetTransactionCount(
        address, DefaultBlockParameterName.LATEST).send().getTransactionCount();
  }

  String tokenSymbol2ContractAddr(String tokenSymbol) {
    ContractInstance contractInstance = tokenSymbolContracts.get(tokenSymbol);
    if (contractInstance == null) {
      throw new RuntimeException("Unknown contract for token " + tokenSymbol);
    }
    return contractInstance.getAddress();
  }

  // convert from basic unit to its minimal unit, e.g., eth -> wei
  private BigInteger basic2MinUnit(String tokenSymbol, String amount) throws Exception {
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
  private BigDecimal min2BasicUnit(String tokenSymbol, BigInteger amount) throws Exception {
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
