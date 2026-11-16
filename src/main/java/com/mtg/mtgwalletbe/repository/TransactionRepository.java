package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Transaction;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    // Search page: fetch the associations the list shows in the same query (avoids N+1 per row)
    @Override
    @EntityGraph(attributePaths = {"payee", "payee.category", "payee.category.parentCategory", "sourceAccount", "targetAccount"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    @Query("SELECT SUM(CASE WHEN t.type = :expenseTransactionType THEN -t.amount ELSE t.amount END) AS profit_loss " +
            "FROM Transaction t " +
            "JOIN t.sourceAccount a " +
            "WHERE t.user.id = :userId " +
            "AND t.dateTime BETWEEN :startDate AND :endDate " +
            "AND t.type IN (:transactionTypes) " +
            "AND a.currency = :currency")
    Optional<BigDecimal> getProfitLossByUserIdAndDateIntervalAndCurrency(@Param("userId") Long userId,
                                                                         @Param("startDate") LocalDateTime startDate,
                                                                         @Param("endDate") LocalDateTime endDate,
                                                                         @Param("currency") Currency currency,
                                                                         @Param("expenseTransactionType") TransactionType expenseTransactionType,
                                                                         @Param("transactionTypes") List<TransactionType> transactionTypes);

    // Startup backfill of searchNotes (SearchTextBackfill); the bulk update skips auditing and @Version
    @Query("SELECT t.id AS id, t.notes AS text FROM Transaction t WHERE t.searchNotes IS NULL AND t.notes IS NOT NULL ORDER BY t.id")
    List<SearchTextSource> findWithoutSearchText(Pageable pageable);

    @Modifying
    @Query("UPDATE Transaction t SET t.searchNotes = :searchText WHERE t.id = :id")
    void setSearchText(@Param("id") Long id, @Param("searchText") String searchText);
}
