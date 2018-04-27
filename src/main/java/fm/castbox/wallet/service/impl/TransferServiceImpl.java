package fm.castbox.wallet.service.impl;

import fm.castbox.wallet.domain.EthAccount;
import fm.castbox.wallet.domain.Transaction;
import fm.castbox.wallet.dto.*;
import fm.castbox.wallet.enumeration.StatusCodeEnum;
import fm.castbox.wallet.enumeration.TransactionEnum;
import fm.castbox.wallet.exception.InvalidParamException;
import fm.castbox.wallet.exception.UserNotExistException;
import fm.castbox.wallet.repository.EthAccountRepository;
import fm.castbox.wallet.repository.TransactionRepository;
import fm.castbox.wallet.service.ContractService;
import fm.castbox.wallet.service.TransferService;
import fm.castbox.wallet.util.APISignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Optional;

@Service
@Slf4j
public class TransferServiceImpl implements TransferService {

  @Autowired
  private EthAccountRepository ethAccountRepository;

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private ContractService contractService;

  @Override
  public TransferRDto<ContractService.TransferEventResponse> transferFromUser(
          String fromUserId, TransferQDto transferQDto) throws Exception {
    if (!transferQDto.getTokenSymbol().equals("ETH") && !transferQDto.getTokenSymbol().equals("BOX")) {
      throw new InvalidParamException(StatusCodeEnum.UNSUPPORTED_SYMBOL, "tokenSymbol",
                                      "unsupported token: " + transferQDto.getTokenSymbol());
    }
    if (!Optional.ofNullable(transferQDto.getToUserId()).isPresent()
            && !Optional.ofNullable(transferQDto.getToAddress()).isPresent()) {
      throw new InvalidParamException(StatusCodeEnum.ADDRESS_OR_USER_ID_MISSING,
              "toUserId & toAddress", "cannot be both missing");
    }
    if (Optional.ofNullable(transferQDto.getToUserId()).isPresent()
            && Optional.ofNullable(transferQDto.getToAddress()).isPresent()) {
      throw new InvalidParamException(StatusCodeEnum.ADDRESS_AND_USER_ID_BOTH_PRESENT,
              "toUserId & toAddress", "cannot be both present");
    }

    APISignUtils.verifySignedObject(transferQDto);

    Optional<EthAccount> fromAccountOptional = ethAccountRepository.findByUserId(fromUserId);
    if (!fromAccountOptional.isPresent()) {
      throw new UserNotExistException(fromUserId, "ETH");
    }
    EthAccount fromEthAccount = fromAccountOptional.get();

    Optional<EthAccount> toAccountOptional;
    if (Optional.ofNullable(transferQDto.getToUserId()).isPresent()) {
      // look up account from toUserId
      toAccountOptional = ethAccountRepository.findByUserId(transferQDto.getToUserId());
      if (!toAccountOptional.isPresent()) {
        throw new UserNotExistException(transferQDto.getToUserId(), "ETH");
      }
    } else {
      // look up account from toAddress
      toAccountOptional = ethAccountRepository.findByAddress(transferQDto.getToAddress());
    }

    if (toAccountOptional.isPresent()) {
      return internalTransfer(fromEthAccount, toAccountOptional.get(),
                                      transferQDto.getTokenSymbol(), transferQDto.getAmount(), transferQDto.getNote());
    } else {
      return externalTransfer(fromEthAccount, transferQDto.getToAddress(),
                                              transferQDto.getTokenSymbol(), transferQDto.getAmount(), transferQDto.getNote());
    }
  }

  // TODO: Fix Transactional Not Working Properly.
  @Transactional
  public TransferRDto internalTransfer(EthAccount fromAccount, EthAccount toAccount,
                                       String symbol, String amount, String note) throws Exception {

    BigInteger transfer_value = contractService.basic2MinUnit(symbol, amount);

    if (transfer_value.compareTo(BigInteger.ZERO) <= 0) {
      throw new InvalidParamException(StatusCodeEnum.TRANSFER_AMOUNT_NEGATIVE, "transfer value", "cannot be negative");
    }

    // reload objects to prevent concurrency problem
    fromAccount = ethAccountRepository.findByUserId(fromAccount.getUserId()).get();
    toAccount = ethAccountRepository.findByUserId(toAccount.getUserId()).get();

    validateTransferableBalance(fromAccount, symbol, transfer_value);

    fromAccount.setBalanceOf(symbol, fromAccount.getBalanceOf(symbol).subtract(transfer_value));
    toAccount.setBalanceOf(symbol, toAccount.getBalanceOf(symbol).add(transfer_value));

    Timestamp now = new Timestamp(System.currentTimeMillis());
    Transaction tx = new Transaction(null, symbol, fromAccount.getUserId(),
            fromAccount.getAddress(), toAccount.getUserId(), toAccount.getAddress(),
            contractService.min2BasicUnit(symbol, transfer_value).toString(), note,
            TransactionEnum.INTERNAL_TYPE, TransactionEnum.DONE_STATE, now, now);

    ethAccountRepository.save(fromAccount);
    ethAccountRepository.save(toAccount);
    transactionRepository.save(tx);

    return new TransferRDto<>(StatusCodeEnum.SUCCESS, "OK", null, tx.getState(), null);
  }

  public TransferRDto externalTransfer(EthAccount fromAccount, String toAddress,
                                       String symbol, String amount, String note) throws Exception {
    BigInteger transfer_value = contractService.basic2MinUnit(symbol, amount);
    validateTransferableBalance(fromAccount, symbol, transfer_value);

    TransferRDto<ContractService.TransferEventResponse> txResponse;
    if (symbol.equals("ETH")) {
      txResponse = contractService.transferEth(toAddress, transfer_value);
    } else {
      txResponse = contractService.transfer(null, contractService.tokenSymbol2ContractAddr(symbol), toAddress, transfer_value);
    }

    recordExternalTransferLocally(symbol, fromAccount, toAddress,
            txResponse.getTxId(), transfer_value, note);
    return txResponse;
  }

  @Transactional
  public void recordExternalTransferLocally(String symbol, EthAccount fromAccount, String toAddress,
                                            String txHash,  BigInteger transfer_value, String note) throws Exception {
    // reload objects to prevent concurrency problem
    fromAccount = ethAccountRepository.findByUserId(fromAccount.getUserId()).get();
    BigInteger balance = fromAccount.getBalanceOf(symbol);
    fromAccount.setBalanceOf(symbol, balance.subtract(transfer_value));
    ethAccountRepository.save(fromAccount);

    Timestamp now = new Timestamp(System.currentTimeMillis());
    Transaction tx =  new Transaction(txHash, symbol, fromAccount.getUserId(),
            fromAccount.getAddress(), null, toAddress,
            contractService.min2BasicUnit(symbol, transfer_value).toString(), note,
            TransactionEnum.EXTERNAL_TYPE, TransactionEnum.PENDING_STATE, now, now);
    transactionRepository.save(tx);
  }

  private void validateTransferableBalance(EthAccount fromAccount, String symbol, BigInteger transferValue) throws Exception {
    BigInteger balance = fromAccount.getBalanceOf(symbol);
    if (balance.compareTo(transferValue) < 0) {
      log.error("Insufficient amount for transfer - uid:" + fromAccount.getUserId()
              + " symbol:" + symbol + " value:" + transferValue + " balance:" + balance);
      throw new InvalidParamException(StatusCodeEnum.INSUFFICIENT_BALANCE, "amount", "Insufficient fund");
    }
  }
}


