package com.mtg.mtgwalletbe.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextTest {

    @ParameterizedTest
    @CsvSource({
            "Isparta, isparta",
            "İZMİR, izmir", // never the two-char "i̇" that ROOT lowercasing gives for İ
            "ısparta, isparta",
            "ŞEHİR ÇAY ÖĞÜ, şehir çay öğü",
            "KÂR Î Û, kâr î û", // capitals with a circumflex, which the DB's lower() skips under the C locale
            "ÉCOLE Ä, école ä",
            "МОСКВА, москва",
            "50%_x\\, 50%_x\\" // wildcards and the escape character pass through unchanged
    })
    void fold_mapsEveryIFormToI_andLowercasesEverythingElse(String input, String expected) {
        assertThat(SearchText.fold(input)).isEqualTo(expected);
    }

    // Decomposed input (macOS/iOS keyboards can send I + U+0307, S + U+0327) folds like the precomposed letters
    @Test
    void fold_composesDecomposedInputFirst() {
        assertThat(SearchText.fold("İZMİR")).isEqualTo(SearchText.fold("İZMİR")).isEqualTo("izmir");
        assertThat(SearchText.fold("ŞEHİR")).isEqualTo("şehir");
        assertThat(SearchText.fold("i̇zmi̇r")).isEqualTo("izmir"); // lowercase i + dot has no NFC form
    }

    // A combining dot on any other letter is kept (NFC composes it: a + U+0307 is ȧ)
    @Test
    void fold_keepsACombiningDotNotAfterAnI() {
        assertThat(SearchText.fold("Ȧ")).isEqualTo("ȧ");
    }

    @Test
    void fold_ofNull_isNull() {
        assertThat(SearchText.fold(null)).isNull();
    }
}
