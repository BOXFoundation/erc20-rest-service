package fm.castbox.wallet.config;

import fm.castbox.wallet.properties.AES256Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class AES256Config {

  private final AES256Properties aes256Properties;

  @Autowired
  public AES256Config(AES256Properties aes256Properties) {
    this.aes256Properties = aes256Properties;
  }

  @Bean
  public TextEncryptor textEncryptor() {
    return Encryptors.text(aes256Properties.getPassword(), aes256Properties.getSalt());
  }
}
