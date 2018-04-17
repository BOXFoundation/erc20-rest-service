package fm.castbox.wallet.service;

import fm.castbox.wallet.domain.EthAccount;
import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.exception.UserNotExistException;
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
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import rx.Subscription;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

/**
 * Our smart contract service.
 */
@Service
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
    PersonalUnlockAccount personalUnlockAccount = admin
        .personalUnlockAccount(nodeProperties.getFromAddress(), walletProperties.getPassphrase())
        .send();
    if ( null == personalUnlockAccount.accountUnlocked() ) {
      throw new Exception("Unlocking account failed");
    }

    web3j = Web3j.build(new HttpService(nodeProperties.getNodeEndpoint()));
  }

  public void subscribeToContractTransferEvents(String contractAddress) {
    if (contractSubscriptions.containsKey(contractAddress)) {
      System.out.println("Already subscribed to contract " + contractAddress);
      return;
    }

    HumanStandardToken humanStandardToken = load(contractAddress);
    Subscription sub = web3j.blockObservable(false).subscribe(block -> {
      BigInteger blockNum = block.getBlock().getNumber();
      DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(blockNum);
      System.out.println("Received block: " + blockNum);
      humanStandardToken.transferEventObservable(blockParameter, blockParameter)
          .subscribe(txEvent -> {
            System.out.println(
                " transfer event :" + txEvent._transactionHash + "\n"
                    + " from    : " + txEvent._from + "\n"
                    + " to      : " + txEvent._to + "\n"
                    + " value   : " + txEvent._value);
            Optional<EthAccount> accountOptional = ethAccountRepository.findByAddress(txEvent._to);
            if (!accountOptional.isPresent()) {
              return;
            }
            EthAccount toEthAccount = accountOptional.get();
            long transferValue = txEvent._value.longValueExact();
            toEthAccount.setBalance(toEthAccount.getBalance() + transferValue);
            ethAccountRepository.save(toEthAccount);
            // Only append tx not saved before. Otherwise previous send tx will be overwritten with main account as from address
            String txId = txEvent._transactionHash;
            if (!transactionRepository.existsByTxId(txId)) {
              Timestamp now = new Timestamp(System.currentTimeMillis());
              transactionRepository.save(new Transaction(txId, "" /* fromUserId */, txEvent._from,
                  "" /* toUserId */, txEvent._to, txEvent._value.longValueExact(), now));
            }
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
      TransactionManager transactionManager = new ClientTransactionManager(
          quorum, nodeProperties.getFromAddress(), privateFor, ATTEMPTS, SLEEP_DURATION);
      HumanStandardToken humanStandardToken = HumanStandardToken.deploy(
          quorum, transactionManager, GAS_PRICE, GAS_LIMIT,
          initialAmount, tokenName, decimalUnits,
          tokenSymbol).send();
      String contractAddress = humanStandardToken.getContractAddress();
      ContractInstance contractInstance = new ContractInstance(contractAddress, initialAmount, tokenName, decimalUnits, tokenSymbol);
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
    ethAccountRepository.save(new EthAccount(userId, address, DUMMY_PRIVATE_KEY, 0, now, now));
    return address;
  }

  public List<BalanceDto> getUserBalances(String userId) {
    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(userId, "BOX");
    }
    double balance = accountOptional.get().getBalance();
    // TODO: fill in dollar amount
    return Arrays.asList(new BalanceDto("BOX", balance, 0));
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
      List<String> privateFor, String tokenSymbol, String fromUserId, String toUserId, String toAddress,
      BigInteger value) throws Exception {
    if (toUserId.isEmpty() && toAddress.isEmpty()) {
      throw new RuntimeException("toUserId and toAddress cannot be both missing");
    }
    if (!toUserId.isEmpty() && !toAddress.isEmpty()) {
      throw new RuntimeException("toUserId and toAddress cannot be both present");
    }
    // look up toAddress from toUserId
    if (!toUserId.isEmpty()) {
      Optional<EthAccount> toAddressOptional = ethAccountRepository.findByUserId(toUserId);
      if (!toAddressOptional.isPresent()) {
        throw new UserNotExistException(toUserId, "ETH");
      }
      toAddress = toAddressOptional.get().getAddress();
    }

    ContractInstance contractInstance = tokenSymbolContracts.get(tokenSymbol);
    if (contractInstance == null) {
      throw new RuntimeException("Unknown contract for token " + tokenSymbol);
    }
    String contractAddress = contractInstance.getAddress();

    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(fromUserId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(fromUserId, "ETH");
    }

    EthAccount fromEthAccount = accountOptional.get();
    long transferValue = value.longValueExact();

    if (fromEthAccount.getBalance() < transferValue) {
      throw new Exception("Insufficient fund");
    }

    // transfer from main account, not fromEthAccount
    TransactionResponse<TransferEventResponse> txResponse =
        transfer(privateFor, contractAddress, toAddress, value);
    fromEthAccount.setBalance(fromEthAccount.getBalance() - transferValue);
    ethAccountRepository.save(fromEthAccount);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    transactionRepository.save(
        new Transaction(txResponse.getTransactionHash(), "" /* fromUserId */, fromEthAccount.getAddress(), "" /* toUserId */, toAddress,
            transferValue, now));
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
    HumanStandardToken humanStandardToken = load(contractAddress, privateFor);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .transfer(to, value).send();
      return processTransferEventsResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
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

  public List<Transaction> listTransactions(String userId) {
    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(userId, "ETH");
    }
    return transactionRepository.findByAddress(accountOptional.get().getAddress());
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
