package com.mtg.mtgwalletbe.security.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

// Real library, no mocks: TotpService only wires it, so the value is in checking the wiring (algorithm, period, PNG)
class TotpServiceTest {

    private final TotpService service = new TotpService();

    /** The code an authenticator app would show right now (30 s period, the service's own clock source). */
    private static String currentCode(String secret) throws Exception {
        long bucket = new SystemTimeProvider().getTime() / 30;
        return new DefaultCodeGenerator().generate(secret, bucket);
    }

    @Test
    void generateSecret_isBase32_andUniquePerCall() {
        String first = service.generateSecret();
        String second = service.generateSecret();

        assertThat(first).matches("[A-Z2-7]+").hasSizeGreaterThanOrEqualTo(16);
        assertThat(first).isNotEqualTo(second);
    }

    // The verifier accepts the neighbouring 30 s window too, so generating "now" and verifying "now" is deterministic
    @Test
    void verifyCode_acceptsTheCurrentCode() throws Exception {
        String secret = service.generateSecret();

        assertThat(service.verifyCode(currentCode(secret), secret)).isTrue();
    }

    @Test
    void verifyCode_rejectsTheCodeOfAnotherSecret() throws Exception {
        String secret = service.generateSecret();
        String otherSecretsCode = currentCode(service.generateSecret());
        // 1 in a million chance of a collision; skip that vanishingly rare case instead of flaking
        org.junit.jupiter.api.Assumptions.assumeFalse(otherSecretsCode.equals(currentCode(secret)));

        assertThat(service.verifyCode(otherSecretsCode, secret)).isFalse();
    }

    @Test
    void verifyCode_rejectsMalformedCodes() {
        String secret = service.generateSecret();

        assertThat(service.verifyCode("", secret)).isFalse();
        assertThat(service.verifyCode("abcdef", secret)).isFalse();
        assertThat(service.verifyCode("12345", secret)).isFalse();
    }

    @Test
    void totpRegex_acceptsExactlySixDigits() {
        assertThat("123456").matches(TotpService.TOTP_REGULAR_EXPRESSION);
        assertThat("12345").doesNotMatch(TotpService.TOTP_REGULAR_EXPRESSION);
        assertThat("1234567").doesNotMatch(TotpService.TOTP_REGULAR_EXPRESSION);
        assertThat("12a456").doesNotMatch(TotpService.TOTP_REGULAR_EXPRESSION);
    }

    @Test
    void qrCode_isBase64EncodedPng() throws Exception {
        String qr = service.getQrCodeImageUri(service.generateSecret(), "alice");

        byte[] png = Base64.getDecoder().decode(qr);
        assertThat(png).startsWith(0x89, 'P', 'N', 'G');
    }
}
