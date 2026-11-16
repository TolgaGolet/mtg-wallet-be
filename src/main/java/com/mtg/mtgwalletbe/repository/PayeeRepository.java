package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayeeRepository extends JpaRepository<Payee, Long>, JpaSpecificationExecutor<Payee> {
    // Search page: the response shows each payee's category, so fetch it in the same query (Payee.category is LAZY)
    @Override
    @EntityGraph(attributePaths = {"category", "category.parentCategory"})
    Page<Payee> findAll(Specification<Payee> spec, Pageable pageable);

    // Single-payee lookups are mapped to PayeeDto, which includes the category: same single query
    @Override
    @EntityGraph(attributePaths = {"category", "category.parentCategory"})
    Optional<Payee> findById(Long id);

    @EntityGraph(attributePaths = {"category", "category.parentCategory"})
    Optional<Payee> findByIdAndStatus(Long id, Status status);

    long countByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT p.name FROM Payee p WHERE p.user.id = :userId AND p.status = :status AND p.id <> :excludeId")
    List<String> findNamesByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Status status, @Param("excludeId") Long excludeId);
}
