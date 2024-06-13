package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c WHERE c.user = :user")
    List<Category> findAllByUser(@Param("user") WalletUser user);
}
