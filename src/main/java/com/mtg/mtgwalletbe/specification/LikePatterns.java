package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.util.SearchText;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Case-insensitive "contains" searches against a pre-folded search column (e.g. {@code searchName}, which entities
 * fill with {@link SearchText#fold} on write). The input gets the same fold, so the comparison needs no SQL function
 * and doesn't depend on the database's locale. User input is matched literally: \, % and _ are escaped, so "50%"
 * doesn't match "500". Blank input means no filter.
 */
final class LikePatterns {
    private static final char ESCAPE = '\\';

    private LikePatterns() {
    }

    static Predicate contains(CriteriaBuilder criteriaBuilder, Expression<String> searchColumn, String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.like(searchColumn, "%" + escape(SearchText.fold(trimmed)) + "%", ESCAPE);
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c == ESCAPE || c == '%' || c == '_') {
                escaped.append(ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
