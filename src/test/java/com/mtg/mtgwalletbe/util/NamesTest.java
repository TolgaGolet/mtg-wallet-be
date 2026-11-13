package com.mtg.mtgwalletbe.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

// CQ-18: name uniqueness is case-insensitive with Turkish rules
class NamesTest {

    @ParameterizedTest
    @CsvSource({
            "Cash, cash, true",
            "İŞ, iş, true",
            "ISPARTA, ısparta, true",
            "' Market ', market, true",
            "Öğrenci Kartı, ÖĞRENCİ KARTI, true",
            "Cash, Card, false",
            "Isparta, isparta, false" // Turkish: lowercase of I is ı, not i
    })
    void sameName(String a, String b, boolean expected) {
        assertThat(Names.sameName(a, b)).isEqualTo(expected);
    }
}
