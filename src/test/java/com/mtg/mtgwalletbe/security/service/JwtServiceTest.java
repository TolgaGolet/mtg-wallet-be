package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.WalletUser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    // Same dummy key as application-unittest.properties
    private static final String SECRET = "dW5pdC10ZXN0LW9ubHktand0LXNpZ25pbmcta2V5LW5vdC1hLXJlYWwtc2VjcmV0";
    private final JwtService jwtService = new JwtService(SECRET, 15, 1440);
    private final WalletUser user = WalletUser.builder().username("alice").build();

    // Regression: generateToken added the "typ" claim to the caller's map
    @Test
    void generateToken_leavesTheCallersClaimsUnchanged() {
        Map<String, Object> claims = new HashMap<>(Map.of("custom", "value"));

        String token = jwtService.generateToken(claims, user);

        assertThat(claims).containsOnlyKeys("custom");
        assertThat(jwtService.parseClaims(token).get("custom", String.class)).isEqualTo("value");
        assertThat(JwtService.tokenTypeOf(jwtService.parseClaims(token))).isEqualTo(JwtService.ACCESS_TOKEN_TYPE);
    }

    @Test
    void generateToken_acceptsAnImmutableMap() {
        String token = jwtService.generateToken(Map.of(), user);

        assertThat(jwtService.parseClaims(token).getSubject()).isEqualTo("alice");
    }
}
