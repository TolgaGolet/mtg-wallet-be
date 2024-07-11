package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AccountSpecification {

    public static Specification<Account> search(AccountSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), request.getId()));
            }
            if (request.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), request.getUserId()));
            }
            if (request.getName() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + request.getName().toLowerCase() + "%"
                ));
            }
            if (request.getTypeValue() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), AccountType.of(request.getTypeValue())));
            }
            if (request.getCurrencyValue() != null) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), Currency.of(request.getCurrencyValue())));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
