package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.CategorySearchRequest;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CategorySpecification {

    public static Specification<Category> search(CategorySearchRequest request, Status status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), request.getId()));
            }
            if (request.getName() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + request.getName().toLowerCase() + "%"
                ));
            }
            if (request.getTransactionTypeValue() != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), TransactionType.of(request.getTransactionTypeValue())));
            }
            if (request.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), request.getUserId()));
            }
            if (request.getParentCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("parentCategory").get("id"), request.getParentCategoryId()));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
