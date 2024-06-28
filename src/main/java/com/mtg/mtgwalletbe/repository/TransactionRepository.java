package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Transaction;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount = :account OR t.targetAccount = :account and t.user = :user")
    Page<Transaction> findUserTransactionsByAccount(@Param("account") Account account, @Param("user") WalletUser user, Pageable pageable);
}
