package fm.castbox.wallet.service.impl;

import fm.castbox.wallet.constant.TransferConsts;
import fm.castbox.wallet.domain.AddressHistory;
import fm.castbox.wallet.domain.EthAccount;
import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.dto.EstFeeQDto;
import fm.castbox.wallet.dto.EstFeeRDto;
import fm.castbox.wallet.enumeration.CommonEnum;
import fm.castbox.wallet.enumeration.StatusCodeEnum;
import fm.castbox.wallet.exception.InvalidParamException;
import fm.castbox.wallet.exception.UserIdAlreadyExistException;
import fm.castbox.wallet.exception.UserNotExistException;
import fm.castbox.wallet.repository.AddressHistoryRepository;
import fm.castbox.wallet.repository.EthAccountRepository;
import fm.castbox.wallet.service.EthService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Timestamp;
import java.util.Optional;

import fm.castbox.wallet.service.Web3jWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Convert;

@Service
@Slf4j
public class EthServiceImpl implements EthService {

  private static final String ADDRESS_PREFIX = "0x";
  private final EthAccountRepository ethAccountRepository;
  private final AddressHistoryRepository addressHistoryRepository;
  private final TextEncryptor textEncryptor;

  @Autowired
  private Web3jWrapper web3jService;

  @Autowired
  public EthServiceImpl(
      EthAccountRepository ethAccountRepository,
      AddressHistoryRepository addressHistoryRepository,
      TextEncryptor textEncryptor) {
    this.ethAccountRepository = ethAccountRepository;
    this.addressHistoryRepository = addressHistoryRepository;
    this.textEncryptor = textEncryptor;
  }

  @Override
  public Optional<String> createAddress(String userId) {
    if (ethAccountRepository.existsByUserId(userId)) {
      throw new UserIdAlreadyExistException(userId, "ETH");
    }
    try {
      ECKeyPair ecKeyPair = Keys.createEcKeyPair();
      String address = ADDRESS_PREFIX + Keys.getAddress(ecKeyPair);
      String privateKey = ecKeyPair.getPrivateKey().toString(16);
      Timestamp now = new Timestamp(System.currentTimeMillis());
      EthAccount ethAccount = new EthAccount(userId, address, privateKey, "0", "0", now, now);
      this.save(ethAccount);
      return Optional.of(address);
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<String> changeAddress(String userId) {
    if (!ethAccountRepository.existsByUserId(userId)) {
      throw new UserNotExistException(userId, "ETH");
    }
    try {
      ECKeyPair ecKeyPair = Keys.createEcKeyPair();
      String address = ADDRESS_PREFIX + Keys.getAddress(ecKeyPair);
      String privateKey = ecKeyPair.getPrivateKey().toString(16);
      Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
      if (!accountOptional.isPresent()) {
        throw new UserNotExistException(userId, "ETH");
      }
      EthAccount ethAccount = accountOptional.get();
      ethAccount.setAddress(address);
      ethAccount.setPrivateKey(privateKey);
      ethAccount.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
      this.save(ethAccount);

      // Move old address to history
      addressHistoryRepository.save(new AddressHistory(userId, "ETH", address, privateKey,
          new Timestamp(System.currentTimeMillis())));
      return Optional.of(address);
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
      return Optional.empty();
    }

  }

  @Override
  public BalanceDto balanceOf(String userId) {
    Optional<EthAccount> accountOptional = ethAccountRepository.findByUserId(userId);
    if (!accountOptional.isPresent()) {
      throw new UserNotExistException(userId, "ETH");
    }
    BigInteger balance = accountOptional.get().getBoxBalance();
    // TODO: fill in dollar amount
    return new BalanceDto("ETH", balance.toString(), "0");
  }

  @Override
  public EstFeeRDto estimateTransferFee(EstFeeQDto estFeeQDto) throws Exception {
    String symbol = estFeeQDto.getSymbol().trim().toUpperCase();
    if (!"BOX".equals(symbol) && !"ETH".equals(symbol)) {
      throw new InvalidParamException(StatusCodeEnum.UNSUPPORTED_SYMBOL, "symbol", "unsupported token: " + symbol);
    }

    BigInteger gasEst = web3jService.estimateTransferGas();
    BigInteger gasPrice = web3jService.ethGasPrice().send().getGasPrice();
    BigInteger gweiFeeEst = gasPrice.multiply(gasEst);
    BigDecimal ethFeeEst = Convert.fromWei(new BigDecimal(gweiFeeEst), Convert.Unit.ETHER);
    BigDecimal symbolFeeEst = BigDecimal.valueOf(0);
    Long timestamp = System.currentTimeMillis();

    if ("ETH".equals(symbol)) {
      symbolFeeEst = ethFeeEst;
    }

    if ("BOX".equals(symbol)) {
      // TODO: query market value of BOX
      symbolFeeEst = ethFeeEst.multiply(TransferConsts.FIXED_ETH_BOX_RATE);
    }

    BigDecimal bdAmount = new BigDecimal(estFeeQDto.getAmount());
    if (bdAmount.compareTo(symbolFeeEst) > 0){
      return new EstFeeRDto(StatusCodeEnum.SUCCESS, CommonEnum.OK,
              symbolFeeEst.toString(), ethFeeEst.toString(), timestamp);
    } else {
      return new EstFeeRDto(StatusCodeEnum.TRANSFER_AMOUNT_LESS_THAN_FEE, "Transfer fee less than amount",
              symbolFeeEst.toString(), ethFeeEst.toString(), timestamp);
    }
  }


  private void save(EthAccount ethAccount) {
    ethAccount.setPrivateKey(textEncryptor.encrypt(ethAccount.getPrivateKey()));
    ethAccountRepository.save(ethAccount);
  }

}
