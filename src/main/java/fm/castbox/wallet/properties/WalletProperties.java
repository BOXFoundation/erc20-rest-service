package fm.castbox.wallet.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("contentbox.wallet")
public class WalletProperties {
    private String passphrase;  // passphrase to encrypt wallet files
    private String dir;  // directory where wallet files reside in
}
