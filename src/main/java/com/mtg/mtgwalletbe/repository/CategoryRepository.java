package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
