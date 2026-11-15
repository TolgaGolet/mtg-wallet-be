package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.WalletUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletUserRepository extends JpaRepository<WalletUser, Long> {
    Optional<WalletUser> findByUsername(String username);

    /** Row-locked read, for read-compare-write changes that concurrent requests must not interleave. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from WalletUser u where u.id = :id")
    Optional<WalletUser> findByIdForUpdate(@Param("id") Long id);

    Optional<WalletUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
