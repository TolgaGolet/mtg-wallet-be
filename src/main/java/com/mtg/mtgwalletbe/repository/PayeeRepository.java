package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayeeRepository extends JpaRepository<Payee, Long>, JpaSpecificationExecutor<Payee> {
    Optional<Payee> findByIdAndStatus(Long id, Status status);

    long countByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT p.name FROM Payee p WHERE p.user.id = :userId AND p.status = :status AND p.id <> :excludeId")
    List<String> findNamesByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Status status, @Param("excludeId") Long excludeId);
}
