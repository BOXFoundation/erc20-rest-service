package fm.castbox.wallet.properties;

import lombok.Data;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Node configuration bean.
 */
@Data
@Configuration
@ConfigurationProperties("contentbox.wallet.api")
public class APIProperties {
    private String allowedPubkey;
}