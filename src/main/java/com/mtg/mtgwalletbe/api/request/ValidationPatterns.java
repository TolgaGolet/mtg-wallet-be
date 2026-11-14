package com.mtg.mtgwalletbe.api.request;

/**
 * Shared validation patterns. Keep in sync with the frontend form rules in mtg-wallet-fe's
 * src/validation/rules.js (its rules.test.js pins these strings).
 */
public final class ValidationPatterns {
    /**
     * Account, category and payee names: letters (incl. Turkish upper/lower case), digits and spaces.
     */
    public static final String NAME_REGULAR_EXPRESSION = "^[a-zA-Z0-9\\sçğıöşüÇĞİÖŞÜ]+$";

    private ValidationPatterns() {
    }
}
