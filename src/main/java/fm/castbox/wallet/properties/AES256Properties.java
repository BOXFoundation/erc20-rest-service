package fm.castbox.wallet.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("contentbox.aes256")
public class AES256Properties {

  private String password;
  private String salt;
}
