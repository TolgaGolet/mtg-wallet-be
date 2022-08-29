package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
