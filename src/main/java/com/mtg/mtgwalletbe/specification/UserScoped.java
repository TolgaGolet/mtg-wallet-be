package com.mtg.mtgwalletbe.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.Objects;

/**
 * Per-user isolation for every search specification: rows of {@code userId} only (the current user, passed by the
 * service; never taken from the request). A missing id fails instead of returning everyone's rows.
 */
public final class UserScoped {

    private UserScoped() {
    }

    public static <T> Specification<T> ownedBy(Long userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), Objects.requireNonNull(userId, "userId"));
    }
}
