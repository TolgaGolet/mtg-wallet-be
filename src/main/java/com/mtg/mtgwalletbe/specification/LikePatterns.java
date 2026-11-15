package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.util.Names;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Case-insensitive "contains" searches. User input is matched literally: % and _ are escaped, so "50%" doesn't
 * match "500".
 */
final class LikePatterns {
    private static final char ESCAPE = '\\';

    private LikePatterns() {
    }

    /**
     * lower(column) LIKE '%input%', with the input normalized like names (trimmed, Turkish lowercase).
     */
    static Predicate contains(CriteriaBuilder criteriaBuilder, Expression<String> column, String input) {
        return criteriaBuilder.like(criteriaBuilder.lower(column), "%" + escape(Names.normalize(input)) + "%", ESCAPE);
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
