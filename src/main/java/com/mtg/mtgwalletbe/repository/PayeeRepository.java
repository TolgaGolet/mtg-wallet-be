package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Payee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayeeRepository extends JpaRepository<Payee, Long> {
}
