package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.PasswordResetToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(WalletUser user);
}
