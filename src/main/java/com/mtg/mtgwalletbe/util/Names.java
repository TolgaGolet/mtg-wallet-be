package com.mtg.mtgwalletbe.util;

import java.util.Collection;
import java.util.Locale;

public final class Names {
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private Names() {
    }

    /**
     * Trims and lowercases with Turkish rules (İ/i, I/ı). Shared by name searches and uniqueness checks.
     */
    public static String normalize(String name) {
        return name.trim().toLowerCase(TURKISH);
    }

    /**
     * Case-insensitive name comparison with Turkish rules, matching how search lowercases names.
     */
    public static boolean sameName(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return normalize(a).equals(normalize(b));
    }

    public static boolean containsName(Collection<String> names, String name) {
        return names.stream().anyMatch(existing -> sameName(existing, name));
    }
}
