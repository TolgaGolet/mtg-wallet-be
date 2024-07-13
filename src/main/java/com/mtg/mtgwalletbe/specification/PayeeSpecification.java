package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.Status;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PayeeSpecification {

    public static Specification<Payee> search(PayeeSearchRequest request, Status status) {
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
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category_id"), request.getId()));
            }
            if (request.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), request.getUserId()));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
