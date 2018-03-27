package fm.castbox.wallet.controller;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import fm.castbox.wallet.domain.Account;
import fm.castbox.wallet.repository.AccountRepository;
import fm.castbox.wallet.config.NodeConfiguration;
import fm.castbox.wallet.dto.TransactionResponse;
import fm.castbox.wallet.service.ContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for our ERC-20 contract API.
 */
@Api("ERC-20 token standard API")
@RestController
public class Controller {
    private final ContractService ContractService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    public Controller(ContractService ContractService) {
        this.ContractService = ContractService;
    }

    @ApiOperation("WalletServerApplication configuration")
    @RequestMapping(value = "/config", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    NodeConfiguration config() {
        return ContractService.getConfig();
    }

    @ApiOperation(
            value = "Generate a new account",
            notes = "Returns hex encoded address of the new account"
    )
    @RequestMapping(value = "/newAccount", method = RequestMethod.POST)
    String newAccount() throws Exception {
        return ContractService.newAccount();
    }

    @ApiOperation(
            value = "Deploy new ERC-20 token",
            notes = "Returns hex encoded contract address")
    @ApiImplicitParam(name = "privateFor",
            value = "Comma separated list of public keys of enclave nodes that transaction is "
                    + "private for",
            paramType = "header",
            dataType = "string")
    @RequestMapping(value = "/deploy", method = RequestMethod.POST)
    String deploy(
            HttpServletRequest request,
            @RequestBody ContractSpecification contractSpecification) throws Exception {

        return ContractService.deploy(
                extractPrivateFor(request),
                contractSpecification.getInitialAmount(),
                contractSpecification.getTokenName(),
                contractSpecification.getDecimalUnits(),
                contractSpecification.getTokenSymbol());
    }

    @ApiOperation("Get token name")
    @RequestMapping(value = "/{contractAddress}/name", method = RequestMethod.GET)
    String name(@PathVariable String contractAddress) throws Exception {
        return ContractService.name(contractAddress);
    }

    @ApiOperation(
            value = "Approve transfers by a specific address up to the provided total quantity",
            notes = "Returns hex encoded transaction hash, and Approval event if called")
    @ApiImplicitParam(name = "privateFor",
            value = "Comma separated list of public keys of enclave nodes that transaction is "
                    + "private for",
            paramType = "header",
            dataType = "string")
    @RequestMapping(value = "/{contractAddress}/approve", method = RequestMethod.POST)
    TransactionResponse<ContractService.ApprovalEventResponse> approve(
            HttpServletRequest request,
            @PathVariable String contractAddress,
            @RequestBody ApproveRequest approveRequest) throws Exception {
        return ContractService.approve(
                extractPrivateFor(request),
                contractAddress,
                approveRequest.getSpender(),
                approveRequest.getValue());
    }

    @ApiOperation("Get total supply of tokens")
    @RequestMapping(value = "/{contractAddress}/totalSupply", method = RequestMethod.GET)
    long totalSupply(@PathVariable String contractAddress) throws Exception {
        return ContractService.totalSupply(contractAddress);
    }

    @ApiOperation(
            value = "Transfer tokens between addresses (must already be approved)",
            notes = "Returns hex encoded transaction hash, and Transfer event if called")
    @ApiImplicitParam(name = "privateFor",
            value = "Comma separated list of public keys of enclave nodes that transaction is "
                    + "private for",
            paramType = "header",
            dataType = "string")
    @RequestMapping(value = "/{contractAddress}/transferFrom", method = RequestMethod.POST)
    TransactionResponse<ContractService.TransferEventResponse> transferFrom(
            HttpServletRequest request,
            @PathVariable String contractAddress,
            @RequestBody TransferFromRequest transferFromRequest) throws Exception {
        Account fromAccount = findByAddress(transferFromRequest.getFrom());
        long transferValue = transferFromRequest.getValue().longValueExact();

        if (fromAccount.getBalance() < transferValue) {
            throw new Exception("Insufficient fund");
        }

        // transfer from main account, not fromAccount
        TransactionResponse<fm.castbox.wallet.service.ContractService.TransferEventResponse> txResponse =
            ContractService.transfer(
                extractPrivateFor(request),
                contractAddress,
                transferFromRequest.getTo(),
                transferFromRequest.getValue());
        fromAccount.setBalance(fromAccount.getBalance() - transferValue);
        accountRepository.save(fromAccount);
        return txResponse;
    }

    @ApiOperation("Get decimal precision of tokens")
    @RequestMapping(value = "/{contractAddress}/decimals", method = RequestMethod.GET)
    long decimals(@PathVariable String contractAddress) throws Exception {
        return ContractService.decimals(contractAddress);
    }

    @ApiOperation("Get contract version")
    @RequestMapping(value = "/{contractAddress}/version", method = RequestMethod.GET)
    String version(@PathVariable String contractAddress) throws Exception {
        return ContractService.version(contractAddress);
    }

    @ApiOperation("Get token balance for address")
    @RequestMapping(
            value = "/{contractAddress}/balanceOf/{ownerAddress}", method = RequestMethod.GET)
    long balanceOf(
            @PathVariable String contractAddress,
            @PathVariable String ownerAddress) throws Exception {

        return findByAddress(ownerAddress).getBalance();
    }

    @ApiOperation("Get token symbol")
    @RequestMapping(value = "/{contractAddress}/symbol", method = RequestMethod.GET)
    String symbol(@PathVariable String contractAddress) throws Exception {
        return ContractService.symbol(contractAddress);
    }

    @ApiOperation(
            value = "Transfer tokens you own to another address",
            notes = "Returns hex encoded transaction hash, and Transfer event if called")
    @ApiImplicitParam(name = "privateFor",
            value = "Comma separated list of public keys of enclave nodes that transaction is "
                    + "private for",
            paramType = "header",
            dataType = "string")
    @RequestMapping(value = "/{contractAddress}/transfer", method = RequestMethod.POST)
    TransactionResponse<ContractService.TransferEventResponse> transfer(
            HttpServletRequest request,
            @PathVariable String contractAddress,
            @RequestBody TransferRequest transferRequest) throws Exception {
        return ContractService.transfer(
                extractPrivateFor(request),
                contractAddress,
                transferRequest.getTo(),
                transferRequest.getValue());
    }

    @ApiOperation(
            value = "Approve transfers by a specific contract address up to the provided total "
                    + "quantity, and notify that contract address of the approval",
            notes = "Returns hex encoded transaction hash, and Approval event if called")
    @ApiImplicitParam(name = "privateFor",
            value = "Comma separated list of public keys of enclave nodes that transaction is "
                    + "private for",
            paramType = "header",
            dataType = "string")
    @RequestMapping(value = "/{contractAddress}/approveAndCall", method = RequestMethod.POST)
    TransactionResponse<ContractService.ApprovalEventResponse> approveAndCall(
            HttpServletRequest request,
            @PathVariable String contractAddress,
            @RequestBody ApproveAndCallRequest approveAndCallRequest) throws Exception {
        return ContractService.approveAndCall(
                extractPrivateFor(request),
                contractAddress,
                approveAndCallRequest.getSpender(),
                approveAndCallRequest.getValue(),
                approveAndCallRequest.getExtraData());
    }

    @ApiOperation("Get quantity of tokens you can transfer on another token holder's behalf")
    @RequestMapping(value = "/{contractAddress}/allowance", method = RequestMethod.GET)
    long allowance(
            @PathVariable String contractAddress,
            @RequestParam String ownerAddress,
            @RequestParam String spenderAddress) throws Exception {
        return ContractService.allowance(
                contractAddress, ownerAddress, spenderAddress);
    }

    @ApiOperation("Returns a list of token transactions for a given account")
    @RequestMapping(value = "/{contractAddress}/listtx/{ownerAddress}", method = RequestMethod.GET)
    List<ContractService.TransferEventResponse> listTransactions(
            @PathVariable String contractAddress,
            @PathVariable String ownerAddress) {
        return ContractService.listTransactions(contractAddress, ownerAddress);
    }

    @ApiOperation(
            value = "Subscribe to token transfer events of a specific contract address",
            notes = "Which can be unsubscribed later")
    @RequestMapping(value = "/subscribe/{contractAddress}", method = RequestMethod.POST)
    void subscribeToContractTransferEvents(@PathVariable String contractAddress) throws Exception {
        ContractService.subscribeToContractTransferEvents(contractAddress);
    }

    @ApiOperation(
            value = "Unsubscribe to token transfer events of a specific contract address",
            notes = "No longer receive this contract's transfer event")
    @RequestMapping(value = "/unsubscribe/{contractAddress}", method = RequestMethod.POST)
    void ubsubscribeToContractTransferEvents(@PathVariable String contractAddress) throws Exception {
        ContractService.unsubscribeToContractTransferEvents(contractAddress);
    }

    private static List<String> extractPrivateFor(HttpServletRequest request) {
        String privateFor = request.getHeader("privateFor");
        if (privateFor == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(privateFor.split(","));
        }
    }

    private Account findByAddress(String address) throws Exception {
        List<Account> accounts = accountRepository.findByAddress(address);

        if (accounts.isEmpty()) {
            throw new Exception("Account with address " + address + " does not exist");
        }
        if (accounts.size() > 1) {
            throw new Exception("Multiple accounts with address " + address);
        }
        return accounts.get(0);
    }

    @Data
    static class ContractSpecification {
        private final BigInteger initialAmount;
        private final String tokenName;
        private final BigInteger decimalUnits;
        private final String tokenSymbol;
    }

    @Data
    static class ApproveRequest {
        private final String spender;
        private final BigInteger value;
    }

    @Data
    static class TransferFromRequest {
        private final String from;
        private final String to;
        private final BigInteger value;
    }

    @Data
    static class TransferRequest {
        private final String to;
        private final BigInteger value;
    }

    @Data
    static class ApproveAndCallRequest {
        private final String spender;
        private final BigInteger value;
        private final String extraData;
    }

    @Data
    static class AllowanceRequest {
        private final String ownerAddress;
        private final String spenderAddress;
    }
}
