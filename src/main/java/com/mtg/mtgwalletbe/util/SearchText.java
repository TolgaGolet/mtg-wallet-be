package com.mtg.mtgwalletbe.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * The fold behind case-insensitive search. Entities store {@code fold(name)} in a search column on write, and search
 * input goes through the same fold, so both sides are compared in Java terms and the database's locale never matters.
 * <p>
 * NFC first (a decomposed "I" + combining dot equals "İ"), then every form of I (I, İ, ı, i) becomes i, then the rest
 * is lowercased with ROOT rules (all scripts). I-forms are mapped before lowercasing because ROOT turns İ into the
 * two-char "i̇". Name uniqueness ({@link Names}) keeps strict Turkish rules instead.
 */
public final class SearchText {
    // A lowercase i plus this dot has no precomposed form, so NFC leaves it; it's dropped after an i instead
    private static final char COMBINING_DOT_ABOVE = '̇';

    private SearchText() {
    }

    public static String fold(String text) {
        if (text == null) {
            return null;
        }
        String composed = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder folded = new StringBuilder(composed.length());
        for (int i = 0; i < composed.length(); i++) {
            char c = composed.charAt(i);
            boolean dotAfterI = c == COMBINING_DOT_ABOVE && !folded.isEmpty() && folded.charAt(folded.length() - 1) == 'i';
            if (!dotAfterI) {
                folded.append(c == 'I' || c == 'İ' || c == 'ı' ? 'i' : c);
            }
        }
        return folded.toString().toLowerCase(Locale.ROOT);
    }
}
