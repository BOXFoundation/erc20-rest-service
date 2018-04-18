package fm.castbox.wallet.controller;

import fm.castbox.wallet.dto.BalanceDto;
import io.swagger.annotations.ApiImplicitParams;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import fm.castbox.wallet.properties.NodeProperties;
import fm.castbox.wallet.domain.Transaction;
import fm.castbox.wallet.dto.TransactionResponse;
import fm.castbox.wallet.service.ContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
    public Controller(ContractService ContractService) {
        this.ContractService = ContractService;
    }

    @ApiOperation("WalletServerApplication configuration")
    @RequestMapping(value = "/0.1/config", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    NodeProperties config() {
        return ContractService.getConfig();
    }

    @ApiOperation(
            value = "Get user's address",
            notes = "Returns hex encoded address of the user"
    )
    @RequestMapping(value = "/0.1/eth/users/{userId}/address", method = RequestMethod.GET)
    String getUserAddress(@PathVariable String userId) throws Exception {
        return ContractService.getUserAddress(userId);
    }

    @ApiOperation(
            value = "Deploy new ERC-20 token",
            notes = "Returns hex encoded contract address")
    @ApiImplicitParam(name = "privateFor",
            value = "Comma separated list of public keys of enclave nodes that transaction is "
                    + "private for",
            paramType = "header",
            dataType = "string")
    @RequestMapping(value = "/0.1/deploy", method = RequestMethod.POST)
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
    @RequestMapping(value = "/0.1/{contractAddress}/name", method = RequestMethod.GET)
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
    @RequestMapping(value = "/0.1/{contractAddress}/approve", method = RequestMethod.POST)
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
    @RequestMapping(value = "/0.1/{contractAddress}/totalSupply", method = RequestMethod.GET)
    long totalSupply(@PathVariable String contractAddress) throws Exception {
        return ContractService.totalSupply(contractAddress);
    }

    @ApiOperation(
            value = "Transfer tokens from a user to an address",
            notes = "Returns hex encoded transaction hash, and Transfer event if called")
    @ApiImplicitParam(name = "privateFor",
            value = "Comma separated list of public keys of enclave nodes that transaction is "
                    + "private for",
            paramType = "header",
            dataType = "string")
    @RequestMapping(value = "/0.1/eth/users/{userId}/transfer", method = RequestMethod.POST)
    TransactionResponse<ContractService.TransferEventResponse> transferFromUser(
            HttpServletRequest request,
            @PathVariable String userId,
            @RequestBody TransferRequest transferRequest) throws Exception {
        return ContractService.transferFromUser(
                extractPrivateFor(request),
                transferRequest.getTokenSymbol(),
                userId,
                transferRequest.getToUserId(),
                transferRequest.getToAddress(),
                transferRequest.getAmount());
    }

    @ApiOperation("Get decimal precision of tokens")
    @RequestMapping(value = "/0.1/{contractAddress}/decimals", method = RequestMethod.GET)
    long decimals(@PathVariable String contractAddress) throws Exception {
        return ContractService.decimals(contractAddress);
    }

    @ApiOperation("Get contract version")
    @RequestMapping(value = "/0.1/{contractAddress}/version", method = RequestMethod.GET)
    String version(@PathVariable String contractAddress) throws Exception {
        return ContractService.version(contractAddress);
    }

    @ApiOperation(
            value = "Get token balances of a user",
            notes = "Returns a dictionary like {“BOX”: “500”, “ETH”: “100”}")
    @RequestMapping(
            value = "/0.1/eth/users/{userId}/balances", method = RequestMethod.GET)
    List<BalanceDto> getUserBalances(@PathVariable String userId) throws Exception {
        return ContractService.getUserBalances(userId);
    }

    @ApiOperation("Get token symbol")
    @RequestMapping(value = "/0.1/{contractAddress}/symbol", method = RequestMethod.GET)
    String symbol(@PathVariable String contractAddress) throws Exception {
        return ContractService.symbol(contractAddress);
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
    @RequestMapping(value = "/0.1/{contractAddress}/approveAndCall", method = RequestMethod.POST)
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
    @RequestMapping(value = "/0.1/{contractAddress}/allowance", method = RequestMethod.GET)
    long allowance(
            @PathVariable String contractAddress,
            @RequestParam String ownerAddress,
            @RequestParam String spenderAddress) throws Exception {
        return ContractService.allowance(
                contractAddress, ownerAddress, spenderAddress);
    }

    @ApiOperation("Returns a list of token transactions for a given user")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "page", dataType = "integer", paramType = "query",
            value = "Results page you want to retrieve (0..N)"),
        @ApiImplicitParam(name = "size", dataType = "integer", paramType = "query",
            value = "Number of records per page."),
        @ApiImplicitParam(name = "sort", allowMultiple = true, dataType = "string", paramType = "query",
            value = "Sorting criteria in the format: property(,asc|desc). " +
                "Default sort order is ascending. " +
                "Multiple sort criteria are supported.")
    })
    @RequestMapping(value = "/0.1/eth/users/{userId}/transactions", method = RequestMethod.GET)
    List<Transaction> listTransactions(
        @PathVariable String userId,
        Pageable pageable) throws Exception {
        return ContractService.listTransactions(userId, pageable);
    }

    @ApiOperation(
            value = "Subscribe to token transfer events of a specific contract address",
            notes = "Which can be unsubscribed later")
    @RequestMapping(value = "/0.1/subscribe/{contractAddress}", method = RequestMethod.POST)
    void subscribeToContractTransferEvents(@PathVariable String contractAddress) throws Exception {
        ContractService.subscribeToContractTransferEvents(contractAddress);
    }

    @ApiOperation(
            value = "Unsubscribe to token transfer events of a specific contract address",
            notes = "No longer receive this contract's transfer event")
    @RequestMapping(value = "/0.1/unsubscribe/{contractAddress}", method = RequestMethod.POST)
    void ubsubscribeToContractTransferEvents(@PathVariable String contractAddress) throws Exception {
        ContractService.unsubscribeToContractTransferEvents(contractAddress);
    }

    @ApiOperation(value = "List all subscribed contract addresses")
    @RequestMapping(value = "/0.1/listContractSubscriptions", method = RequestMethod.GET)
    List<String> listContractSubscriptions() throws Exception {
        return ContractService.listContractSubscriptions();
    }

    private static List<String> extractPrivateFor(HttpServletRequest request) {
        String privateFor = request.getHeader("privateFor");
        if (privateFor == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(privateFor.split(","));
        }
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
    static class TransferRequest {
        private final String tokenSymbol;
        private final String toUserId;
        private final String toAddress;
        private final BigInteger amount;
        // signature
        private final String sign;
        // to prevent replay attack
        private final long timestamp;
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
