package fm.castbox.wallet.service.impl;

import fm.castbox.wallet.domain.EthAccount;
import fm.castbox.wallet.domain.AddressHistory;
import fm.castbox.wallet.exception.UserIdAlreadyExistException;
import fm.castbox.wallet.exception.UserNotExistException;
import fm.castbox.wallet.repository.EthAccountRepository;
import fm.castbox.wallet.repository.AddressHistoryRepository;
import fm.castbox.wallet.service.EthAccountService;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

@Service
public class EthAccountServiceImpl implements EthAccountService {
  private static final String ADDRESS_PREFIX = "0x";
  private final EthAccountRepository ethAccountRepository;
  private final AddressHistoryRepository addressHistoryRepository;

  @Autowired
  public EthAccountServiceImpl(EthAccountRepository ethAccountRepository,
      AddressHistoryRepository addressHistoryRepository) {
    this.ethAccountRepository = ethAccountRepository;
    this.addressHistoryRepository = addressHistoryRepository;
  }

  @Override
  public Optional<String> createAddress(String userId) {
    boolean exist = ethAccountRepository.existsByUserId(userId);
    if (exist) {
      throw new UserIdAlreadyExistException(userId, "ETH");
    }
    try {
      ECKeyPair ecKeyPair = Keys.createEcKeyPair();
      String address = ADDRESS_PREFIX + Keys.getAddress(ecKeyPair);
      String privateKey = ecKeyPair.getPrivateKey().toString(16);
      Timestamp now = new Timestamp(System.currentTimeMillis());
      EthAccount ethAccount = new EthAccount(userId, address, privateKey, 0, now, now);
      ethAccountRepository.save(ethAccount);
      return Optional.of(address);
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<String> changeAddress(String userId) {
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
      ethAccountRepository.save(ethAccount);

      // Move old address to history
      addressHistoryRepository.save(new AddressHistory(userId, "ETH", address, privateKey,
          new Timestamp(System.currentTimeMillis())));
      return Optional.of(address);
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
      return Optional.empty();
    }

  }


}
