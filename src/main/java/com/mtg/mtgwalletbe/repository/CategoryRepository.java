package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    @Query("SELECT c FROM Category c WHERE c.user = :user AND c.status = :status")
    List<Category> findAllByUserAndStatus(@Param("user") WalletUser user, @Param("status") Status status);

    Optional<Category> findByIdAndStatus(Long id, Status status);
}
