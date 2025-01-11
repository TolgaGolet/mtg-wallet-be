package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.TransactionSearchRequest;
import com.mtg.mtgwalletbe.entity.Transaction;
import com.mtg.mtgwalletbe.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<Transaction> search(TransactionSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), request.getId()));
            }
            if (request.getTypeValue() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), TransactionType.of(request.getTypeValue())));
            }
            if (request.getPayeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("payee").get("id"), request.getPayeeId()));
            }
            if (request.getPayeeName() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("payee").get("name")),
                        "%" + request.getPayeeName().trim().toLowerCase() + "%"
                ));
            }
            if (request.getAmount() != null) {
                predicates.add(criteriaBuilder.equal(root.get("amount"), request.getAmount()));
            }
            if (request.getDateTime() != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("DATE", LocalDateTime.class, root.get("dateTime")),
                        criteriaBuilder.function("DATE", LocalDateTime.class, criteriaBuilder.literal(request.getDateTime()))
                ));
            }
            if (request.getSourceAccountId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceAccount").get("id"), request.getSourceAccountId()));
            }
            if (request.getTargetAccountId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetAccount").get("id"), request.getTargetAccountId()));
            }
            if (request.getNotes() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("notes")),
                        "%" + request.getNotes().trim().toLowerCase() + "%"
                ));
            }
            if (request.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), request.getUserId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
