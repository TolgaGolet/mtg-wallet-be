package com.mtg.mtgwalletbe.api.request;

/**
 * Shared validation patterns. Keep in sync with the frontend form rules (mtg-wallet-fe).
 */
public final class ValidationPatterns {
    /**
     * Account, category and payee names: letters (incl. Turkish upper/lower case), digits and spaces.
     */
    public static final String NAME_REGULAR_EXPRESSION = "^[a-zA-Z0-9\\sçğıöşüÇĞİÖŞÜ]+$";

    private ValidationPatterns() {
    }
}
