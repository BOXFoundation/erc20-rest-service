package io.blk.erc20.repository;

import io.blk.erc20.domain.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountRepository extends CrudRepository<Account, Long> {
    List<Account> findByAddress(String address);
}