package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayeeRepository extends JpaRepository<Payee, Long>, JpaSpecificationExecutor<Payee> {
    @Query("SELECT p FROM Payee p WHERE p.user = :user AND p.status = :status")
    List<Payee> findAllByUserAndStatus(@Param("user") WalletUser user, @Param("status") Status status);

    Optional<Payee> findByIdAndStatus(Long id, Status status);
}
