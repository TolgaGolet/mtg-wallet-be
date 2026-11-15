package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.OneTimeToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface OneTimeTokenRepository<T extends OneTimeToken> extends JpaRepository<T, Long> {
    Optional<T> findByToken(String token);

    List<T> findAllByUser(WalletUser user);
}
