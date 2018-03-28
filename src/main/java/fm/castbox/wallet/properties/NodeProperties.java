package fm.castbox.wallet.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Node configuration bean.
 */
@Data
@ConfigurationProperties("contentbox.fullnode")
public class NodeProperties {

    private String nodeEndpoint;
    private String fromAddress;
}
