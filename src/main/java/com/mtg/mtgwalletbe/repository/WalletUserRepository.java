package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletUserRepository extends JpaRepository<WalletUser, Long> {
    Optional<WalletUser> findByUsername(String username);
}
