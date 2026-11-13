package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    List<Account> findAllByUserIdAndStatus(Long userId, Status status);

    Optional<Account> findByIdAndStatus(Long id, Status status);

    long countByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT a.name FROM Account a WHERE a.user.id = :userId AND a.status = :status AND a.id <> :excludeId")
    List<String> findNamesByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Status status, @Param("excludeId") Long excludeId);
}
