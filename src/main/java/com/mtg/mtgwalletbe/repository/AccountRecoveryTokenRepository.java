package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.AccountRecoveryToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRecoveryTokenRepository extends JpaRepository<AccountRecoveryToken, Long> {
    Optional<AccountRecoveryToken> findByToken(String token);

    Optional<AccountRecoveryToken> findByUser(WalletUser user);
}
