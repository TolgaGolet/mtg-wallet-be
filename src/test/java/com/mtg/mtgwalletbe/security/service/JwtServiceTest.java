package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.WalletUser;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

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

    @Test
    void accessToken_carriesTheUsersRoles() {
        WalletUser admin = WalletUser.builder().username("root")
                .roles(new java.util.HashSet<>(java.util.Set.of(new com.mtg.mtgwalletbe.entity.Role(1L, "ADMIN")))).build();

        Claims claims = jwtService.parseClaims(jwtService.generateToken(admin));

        assertThat(claims.get("roles", java.util.List.class)).containsExactly("ADMIN");
    }

    // A UserDetails without authorities (null) still gets a token, just without a roles claim
    @Test
    void accessToken_forUserWithNullAuthorities_hasNoRolesClaim() {
        UserDetails noAuthorities = org.mockito.Mockito.mock(UserDetails.class);
        org.mockito.Mockito.when(noAuthorities.getUsername()).thenReturn("bob");
        org.mockito.Mockito.when(noAuthorities.getAuthorities()).thenReturn(null);

        Claims claims = jwtService.parseClaims(jwtService.generateToken(noAuthorities));

        assertThat(claims.getSubject()).isEqualTo("bob");
        assertThat(claims).doesNotContainKey("roles");
    }

    @Test
    void refreshToken_isTypedRefresh_andOutlivesTheAccessToken() {
        Claims access = jwtService.parseClaims(jwtService.generateToken(user));
        Claims refresh = jwtService.parseClaims(jwtService.generateRefreshToken(user));

        assertThat(JwtService.tokenTypeOf(refresh)).isEqualTo(JwtService.REFRESH_TOKEN_TYPE);
        assertThat(refresh.getSubject()).isEqualTo("alice");
        assertThat(refresh).doesNotContainKey("roles");
        // 15 min vs. 1440 min (constructor arguments above)
        assertThat(refresh.getExpiration()).isAfter(access.getExpiration());
        long accessMinutes = (access.getExpiration().getTime() - access.getIssuedAt().getTime()) / 60_000;
        long refreshMinutes = (refresh.getExpiration().getTime() - refresh.getIssuedAt().getTime()) / 60_000;
        assertThat(accessMinutes).isEqualTo(15);
        assertThat(refreshMinutes).isEqualTo(1440);
    }

    @Test
    void tokensIssuedInTheSameInstant_areStillDistinct() {
        assertThat(jwtService.generateToken(user)).isNotEqualTo(jwtService.generateToken(user));
    }

    @Test
    void isTokenValid_forTheSubject_isTrue() {
        Claims claims = jwtService.parseClaims(jwtService.generateToken(user));

        assertThat(jwtService.isTokenValid(claims, user)).isTrue();
    }

    @Test
    void isTokenValid_forAnotherUsername_isFalse() {
        Claims claims = jwtService.parseClaims(jwtService.generateToken(user));

        assertThat(jwtService.isTokenValid(claims, WalletUser.builder().username("mallory").build())).isFalse();
    }

    // parseClaims rejects expired tokens itself, so the expiry branch is checked on claims built directly
    @Test
    void isTokenValid_withPastExpiration_isFalse() {
        Claims expired = io.jsonwebtoken.Jwts.claims().subject("alice")
                .expiration(new java.util.Date(System.currentTimeMillis() - 1_000)).build();

        assertThat(jwtService.isTokenValid(expired, user)).isFalse();
    }

    @Test
    void parseClaims_rejectsATokenSignedWithAnotherKey() {
        JwtService otherKey = new JwtService("b3RoZXItdW5pdC10ZXN0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1oczI1Ng==", 15, 1440);
        String forged = otherKey.generateToken(user);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jwtService.parseClaims(forged))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    void parseClaims_rejectsAnExpiredToken() {
        JwtService alreadyExpired = new JwtService(SECRET, -1, -1);
        String token = alreadyExpired.generateToken(user);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
