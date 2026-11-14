package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.util.Names;
import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PayeeSpecification {

    /**
     * Always scoped to userId via {@link UserScoped#ownedBy}.
     */
    public static Specification<Payee> search(PayeeSearchRequest request, Status status, Long userId) {
        return UserScoped.<Payee>ownedBy(userId).and((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), request.getId()));
            }
            if (request.getName() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + Names.normalize(request.getName()) + "%"
                ));
            }
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), request.getCategoryId()));
            }
            if (request.getTransactionTypeValue() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("transactionType"), TransactionType.of(request.getTransactionTypeValue())));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
