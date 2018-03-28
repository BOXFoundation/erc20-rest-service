package fm.castbox.wallet.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import fm.castbox.wallet.domain.Account;
import fm.castbox.wallet.domain.Transaction;
import fm.castbox.wallet.generated.HumanStandardToken;
import fm.castbox.wallet.config.NodeConfiguration;
import fm.castbox.wallet.dto.TransactionResponse;
import fm.castbox.wallet.repository.AccountRepository;
import fm.castbox.wallet.repository.TransactionRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
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

    private final Quorum quorum;

    private final NodeConfiguration nodeConfiguration;

    private final Admin admin;

    private final Web3j web3j;

    private HashMap<String /* contract address */, Subscription /* contract event subscription */> contractSubscriptions = new HashMap<>();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    public ContractService(Quorum quorum, NodeConfiguration nodeConfiguration) throws Exception {
        this.quorum = quorum;
        this.nodeConfiguration = nodeConfiguration;

        admin = Admin.build(new HttpService(nodeConfiguration.getNodeEndpoint()));
        PersonalUnlockAccount personalUnlockAccount = admin.personalUnlockAccount(nodeConfiguration.getFromAddress(), nodeConfiguration.getEncryptPassphrase()).send();
        if (!personalUnlockAccount.accountUnlocked()) {
            throw new Exception("Unlocking account failed");
        }

        web3j = Web3j.build(new HttpService(nodeConfiguration.getNodeEndpoint()));
    }

    public void subscribeToContractTransferEvents(String contractAddress) throws Exception {
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
                        List<Account> accounts = accountRepository.findByAddress(txEvent._to);
                        if (accounts.isEmpty()) {
                            return;
                        }
                        if (accounts.size() > 1) {
                            System.out.println("Multiple accounts with address " + txEvent._to);
                            return;
                        }
                        Account toAccount = accounts.get(0);
                        long transferValue = txEvent._value.longValueExact();
                        toAccount.setBalance(toAccount.getBalance() + transferValue);
                        accountRepository.save(toAccount);
                        // Only append tx not saved before. Otherwise previous send tx will be overwritten with main account as from address
                        String txId = txEvent._transactionHash;
                        if (!transactionRepository.existsById(txId)) {
                            transactionRepository.save(new Transaction(txId, txEvent._from, txEvent._to, txEvent._value.longValueExact()));
                        }
                    });
        }, Throwable::printStackTrace);
        contractSubscriptions.put(contractAddress, sub);
        System.out.println("Subscribed to contract " + contractAddress);
    }

    public void unsubscribeToContractTransferEvents(String contractAddress) throws Exception {
        if (!contractSubscriptions.containsKey(contractAddress)) {
            System.out.println("Trying to unsubscribe from non-subscribed contract " + contractAddress);
            return;
        }
        Subscription sub = contractSubscriptions.remove(contractAddress);
        sub.unsubscribe();
        System.out.println("Unsubscribed from contract " + contractAddress);
    }

    public List<String> listContractSubscriptions() throws Exception {
        return new ArrayList<>(contractSubscriptions.keySet());
    }

    public NodeConfiguration getConfig() {
        return nodeConfiguration;
    }

    public String deploy(
            List<String> privateFor, BigInteger initialAmount, String tokenName, BigInteger decimalUnits,
            String tokenSymbol) throws Exception {
        try {
            TransactionManager transactionManager = new ClientTransactionManager(
                    quorum, nodeConfiguration.getFromAddress(), privateFor);
            HumanStandardToken humanStandardToken = HumanStandardToken.deploy(
                    quorum, transactionManager, GAS_PRICE, GAS_LIMIT,
                    initialAmount, tokenName, decimalUnits,
                    tokenSymbol).send();
            return humanStandardToken.getContractAddress();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserAddress(String userId) throws Exception {
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.size() > 1) {
            throw new Exception("Multiple accounts with user id " + userId);
        }
        if (accounts.size() == 1) {
            return accounts.get(0).getAddress();
        }
        // create a new account for user
        String address = admin.personalNewAccount(nodeConfiguration.getEncryptPassphrase()).send().getAccountId();
        // initial balance 0
        accountRepository.save(new Account(userId, address, 0));
        return address;
    }

    public long getUserBalance(String userId) throws Exception {
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.size() > 1) {
            throw new Exception("Multiple accounts with user id " + userId);
        }
        if (accounts.isEmpty()) {
            throw new Exception("Account with user id " + userId + " does not exist");
        }
        return accounts.get(0).getBalance();
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
            List<String> privateFor, String contractAddress, String spender, BigInteger value) throws Exception {
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

    public TransactionResponse<TransferEventResponse> transferFrom(
            List<String> privateFor, String contractAddress, String fromUserId, String toAddress, BigInteger value) throws Exception {
             List<Account> accounts = accountRepository.findByUserId(fromUserId);
            if (accounts.size() > 1) {
                throw new Exception("Multiple accounts with user id " + fromUserId);
            }
            if (accounts.isEmpty()) {
                throw new Exception("Account with user id " + fromUserId + " does not exist");
            }

            Account fromAccount = accounts.get(0);
            long transferValue = value.longValueExact();

            if (fromAccount.getBalance() < transferValue) {
                throw new Exception("Insufficient fund");
            }

            // transfer from main account, not fromAccount
            TransactionResponse<TransferEventResponse> txResponse =
                    transfer(privateFor, contractAddress, toAddress, value);
            fromAccount.setBalance(fromAccount.getBalance() - transferValue);
            accountRepository.save(fromAccount);
            transactionRepository.save(new Transaction(txResponse.getTransactionHash(), fromAccount.getAddress(), toAddress, transferValue));
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
            List<String> privateFor, String contractAddress, String to, BigInteger value) throws Exception {
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

    public long allowance(String contractAddress, String ownerAddress, String spenderAddress) throws Exception {
        HumanStandardToken humanStandardToken = load(contractAddress);
        try {
            return extractLongValue(humanStandardToken.allowance(
                    ownerAddress, spenderAddress)
                    .send());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Transaction> listTransactions(String contractAddress, String userId) throws Exception {
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.size() > 1) {
            throw new Exception("Multiple accounts with user id " + userId);
        }
        if (accounts.isEmpty()) {
            throw new Exception("Account with user id " + userId + " does not exist");
        }
        return transactionRepository.findByAddress(accounts.get(0).getAddress());
    }

    private HumanStandardToken load(String contractAddress, List<String> privateFor) {
        TransactionManager transactionManager = new ClientTransactionManager(
                quorum, nodeConfiguration.getFromAddress(), privateFor);
        return HumanStandardToken.load(
                contractAddress, quorum, transactionManager, GAS_PRICE, GAS_LIMIT);
    }

    private HumanStandardToken load(String contractAddress) {
        TransactionManager transactionManager = new ClientTransactionManager(
                quorum, nodeConfiguration.getFromAddress(), Collections.emptyList());
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

        public TransferEventResponse() { }

        public TransferEventResponse(
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

        public ApprovalEventResponse() { }

        public ApprovalEventResponse(
                HumanStandardToken.ApprovalEventResponse approvalEventResponse) {
            this.owner = approvalEventResponse._owner;
            this.spender = approvalEventResponse._spender;
            this.value = approvalEventResponse._value.longValueExact();
        }
    }
}
