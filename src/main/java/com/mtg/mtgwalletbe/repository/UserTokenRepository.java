package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    @Query(value = """
            select ut from UserToken ut inner join WalletUser wu\s
            on ut.user.id = wu.id\s
            where wu.id = :id and (ut.expired = false or ut.revoked = false)\s
            """)
    List<UserToken> findAllValidUserTokensByUser(Long id);

    Optional<UserToken> findByToken(String token);
}
