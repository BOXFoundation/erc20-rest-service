package fm.castbox.wallet.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Node configuration bean.
 */
@Data
@Configuration
@ConfigurationProperties("contentbox.fullnode")
public class NodeProperties {
    private String nodeEndpoint;
    private String fromAddress;
}