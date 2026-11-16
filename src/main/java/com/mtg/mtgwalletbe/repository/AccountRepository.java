package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    Optional<Account> findByIdAndStatus(Long id, Status status);

    long countByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT a.name FROM Account a WHERE a.user.id = :userId AND a.status = :status AND a.id <> :excludeId")
    List<String> findNamesByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Status status, @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId AND a.status = :status AND a.currency = :currency")
    BigDecimal sumBalanceByUserIdAndStatusAndCurrency(@Param("userId") Long userId, @Param("status") Status status, @Param("currency") Currency currency);

    // Startup backfill of searchName (SearchTextBackfill); the bulk update skips auditing and @Version
    @Query("SELECT a.id AS id, a.name AS text FROM Account a WHERE a.searchName IS NULL ORDER BY a.id")
    List<SearchTextSource> findWithoutSearchText(Pageable pageable);

    @Modifying
    @Query("UPDATE Account a SET a.searchName = :searchText WHERE a.id = :id")
    void setSearchText(@Param("id") Long id, @Param("searchText") String searchText);
}
