package fm.castbox.wallet;

import fm.castbox.wallet.properties.NodeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.protocol.ipc.WindowsIpcService;
import org.web3j.quorum.Quorum;


@SpringBootApplication
public class WalletServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServerApplication.class, args);
    }

    @Autowired
    NodeProperties nodeProperties;

    @Bean
    Quorum quorum() {
        String nodeEndpoint = nodeProperties.getNodeEndpoint();
        Web3jService web3jService;
        if (nodeEndpoint == null || nodeEndpoint.equals("")) {
            web3jService = new HttpService();
        } else if (nodeEndpoint.startsWith("http")) {
            web3jService = new HttpService(nodeEndpoint);
        } else if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            web3jService = new WindowsIpcService(nodeEndpoint);
        } else {
            web3jService = new UnixIpcService(nodeEndpoint);
        }
        return Quorum.build(web3jService);
    }

}
