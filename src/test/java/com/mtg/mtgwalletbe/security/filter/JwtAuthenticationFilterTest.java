package com.mtg.mtgwalletbe.security.filter;

import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.TokenType;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import com.mtg.mtgwalletbe.security.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // Regression: the whitelist used equals(), so wildcard entries like /auth/totp/* never matched
    @ParameterizedTest
    @CsvSource({
            "/auth/totp/verify, true",
            "/auth/totp/setup, true",
            "/auth/authenticate, true",
            "/actuator/health, true",
            "/account/search, false",
            "/auth/totp/setup/extra, false"
    })
    void whitelistMatchesPatterns(String path, boolean expected) {
        assertThat(JwtAuthenticationFilter.isWhitelisted(path)).isEqualTo(expected);
    }

    // Refresh tokens are stored as active UserToken rows too; only BEARER rows authenticate API calls
    @ParameterizedTest
    @CsvSource({"BEARER, true", "REFRESH, false"})
    void onlyActiveAccessTokenRowsAuthenticate(TokenType rowType, boolean authenticated) throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        UserTokenRepository userTokenRepository = mock(UserTokenRepository.class);
        WalletUser user = WalletUser.builder().id(1L).username("alice").build();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("alice");
        when(jwtService.parseClaims("tok")).thenReturn(claims);
        when(jwtService.isTokenValid(claims, user)).thenReturn(true);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);
        when(userTokenRepository.findByToken("tok")).thenReturn(Optional.of(UserToken.builder().token("tok").tokenType(rowType).build()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account/search");
        request.setServletPath("/account/search");
        request.addHeader("Authorization", "Bearer tok");

        new JwtAuthenticationFilter(jwtService, userDetailsService, userTokenRepository)
                .doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication() != null).isEqualTo(authenticated);
    }
}
