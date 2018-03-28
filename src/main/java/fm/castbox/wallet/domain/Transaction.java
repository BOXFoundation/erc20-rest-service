package fm.castbox.wallet.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Transaction {
    private @Id String txId;
    // Note: for withdraw, this is sender's address. However, it is our main account address on chain as viewed in a block explorer
    private String fromAddress;
    private String toAddress;
    private long value;
}