package fm.castbox.wallet.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({NodeProperties.class, WalletProperties.class})
public class PropertiesConfig {
}
