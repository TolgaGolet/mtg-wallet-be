package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PayeeRepository extends JpaRepository<Payee, Long> {
    @Query("SELECT p FROM Payee p WHERE p.user = :user")
    List<Payee> findAllByUser(@Param("user") WalletUser user);
}
