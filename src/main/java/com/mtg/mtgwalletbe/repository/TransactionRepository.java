package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
