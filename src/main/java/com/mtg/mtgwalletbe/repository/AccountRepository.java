package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    @Query("SELECT a FROM Account a WHERE a.user = :user AND a.status = :status")
    List<Account> findAllByUserAndStatus(@Param("user") WalletUser user, @Param("status") Status status);

    Optional<Account> findByIdAndStatus(Long id, Status status);
}
