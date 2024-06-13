package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @Query("SELECT a FROM Account a WHERE a.user = :user")
    List<Account> findAllByUser(@Param("user") WalletUser user);
}
