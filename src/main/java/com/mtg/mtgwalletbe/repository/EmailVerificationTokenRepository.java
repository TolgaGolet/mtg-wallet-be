package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.EmailVerificationToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findAllByUser(WalletUser user);
}
