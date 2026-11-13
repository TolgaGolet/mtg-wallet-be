package com.mtg.mtgwalletbe.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskingTest {

    @Test
    void maskEmail_masksLocalPart() {
        assertThat(LogMasking.maskEmail("john.doe@example.com")).isEqualTo("j***@example.com");
    }

    @Test
    void maskEmail_singleCharLocalPart() {
        assertThat(LogMasking.maskEmail("j@example.com")).isEqualTo("j***@example.com");
    }

    @Test
    void maskEmail_invalidInputs() {
        assertThat(LogMasking.maskEmail(null)).isEqualTo("***");
        assertThat(LogMasking.maskEmail("no-at-sign")).isEqualTo("***");
        assertThat(LogMasking.maskEmail("@example.com")).isEqualTo("***");
        assertThat(LogMasking.maskEmail("john@")).isEqualTo("***");
    }
}
