package com.mtg.mtgwalletbe.util;

public final class LogMasking {
    private LogMasking() {
    }

    /**
     * Masks an email address for logging, e.g. "john@example.com" -> "j***@example.com".
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
