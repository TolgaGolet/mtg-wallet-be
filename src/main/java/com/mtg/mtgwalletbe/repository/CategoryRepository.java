package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByIdAndStatus(Long id, Status status);

    boolean existsByParentCategoryIdAndStatus(Long parentCategoryId, Status status);

    long countByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT c.name FROM Category c WHERE c.user.id = :userId AND c.status = :status AND c.transactionType = :type AND c.id <> :excludeId")
    List<String> findNamesByUserIdAndStatusAndType(@Param("userId") Long userId, @Param("status") Status status, @Param("type") TransactionType type, @Param("excludeId") Long excludeId);

    List<Category> findAllByUserIdAndStatus(Long userId, Status status);
}
