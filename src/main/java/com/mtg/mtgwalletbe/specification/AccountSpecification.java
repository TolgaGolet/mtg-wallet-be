package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AccountSpecification {

    /**
     * Always scoped to userId via {@link UserScoped#ownedBy}.
     */
    public static Specification<Account> search(AccountSearchRequest request, Status status, Long userId) {
        return UserScoped.<Account>ownedBy(userId).and((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), request.getId()));
            }
            if (request.getName() != null) {
                predicates.add(LikePatterns.contains(criteriaBuilder, root.get("searchName"), request.getName()));
            }
            if (request.getTypeValue() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), AccountType.of(request.getTypeValue())));
            }
            if (request.getCurrencyValue() != null) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), Currency.of(request.getCurrencyValue())));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
