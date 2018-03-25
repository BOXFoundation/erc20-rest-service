package io.blk.erc20.domain;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class Account {

    private @Id @GeneratedValue Long id;
    private String address;
    private long balance;

    private Account() {}

    public Account(String address, long balance) {
        this.address = address;
        this.balance = balance;
    }
}