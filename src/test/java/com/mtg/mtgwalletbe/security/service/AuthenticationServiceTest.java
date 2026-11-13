package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.TokenType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.security.api.response.AuthenticationResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Regression for CQ-11: one bearer-token resolver with token-type and revocation checks
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationServiceTest {

    private static final String TOKEN = "header.payload.signature";

    @Mock private AuthenticationManager authenticationManager;
    @Mock private WalletUserRepository walletUserRepository;
    @Mock private UserTokenRepository userTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private TotpService totpService;
    @InjectMocks private AuthenticationService service;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Claims claims = mock(Claims.class);
    private WalletUser user;

    @BeforeEach
    void setUp() {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        user = WalletUser.builder().id(1L).username("alice").build();
        when(jwtService.parseClaims(TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("alice");
        when(walletUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(claims, user)).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
    }

    private static GenericExceptionMessages errorOf(Throwable e) {
        return ((MtgWalletGenericException) e).getError();
    }

    private UserToken storedToken(TokenType type, boolean revoked) {
        return UserToken.builder().token(TOKEN).tokenType(type).user(user).revoked(revoked).expired(revoked).build();
    }

    @Test
    void refresh_withRefreshToken_issuesNewTokens_andStoresTheNewRefreshToken() throws Exception {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.REFRESH, false)));

        AuthenticationResponse response = service.refreshToken(request, new MockHttpServletResponse());

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        ArgumentCaptor<UserToken> saved = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(UserToken::getToken, UserToken::getTokenType)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("new-access", TokenType.BEARER),
                        org.assertj.core.groups.Tuple.tuple("new-refresh", TokenType.REFRESH));
    }

    // Regression: refresh tokens weren't stored, so logout / account deletion couldn't revoke them
    @Test
    void refresh_withoutStoredRow_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refreshToken(request, new MockHttpServletResponse()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    @Test
    void refresh_withRevokedRefreshToken_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.REFRESH, true)));

        assertThatThrownBy(() -> service.refreshToken(request, new MockHttpServletResponse()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
        verify(jwtService, never()).generateToken(any());
    }

    // Regression: the user came from the token's username, which a deleted account frees for someone else
    @Test
    void refresh_issuesTokensForTheTokenOwner_notByUsernameLookup() throws Exception {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.REFRESH, false)));
        when(walletUserRepository.findByUsername("alice")).thenReturn(Optional.of(WalletUser.builder().id(2L).username("alice").build()));

        service.refreshToken(request, new MockHttpServletResponse());

        verify(jwtService).generateToken(user);
        verify(walletUserRepository, never()).findByUsername(any());
    }

    @Test
    void refresh_withAccessTokenByTypeClaim_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.ACCESS_TOKEN_TYPE);

        assertThatThrownBy(() -> service.refreshToken(request, new MockHttpServletResponse()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    // Tokens issued before the "typ" claim existed: access tokens are recognised by their UserToken row
    @Test
    void refresh_withLegacyAccessTokenWithoutTypeClaim_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(null);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.BEARER, false)));

        assertThatThrownBy(() -> service.refreshToken(request, new MockHttpServletResponse()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    @Test
    void totpSetup_withRevokedAccessToken_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.ACCESS_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.BEARER, true)));

        assertThatThrownBy(() -> service.setupTotp(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
        verify(totpService, never()).generateSecret();
    }

    @Test
    void totpSetup_withRefreshToken_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);

        assertThatThrownBy(() -> service.setupTotp(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    // Refresh rows are active UserTokens too; they must not pass as access tokens
    @Test
    void totpSetup_withStoredRefreshTokenWithoutTypeClaim_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(null);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.REFRESH, false)));

        assertThatThrownBy(() -> service.setupTotp(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    @Test
    void missingHeader_isRejected() {
        MockHttpServletRequest noHeader = new MockHttpServletRequest();

        assertThatThrownBy(() -> service.refreshToken(noHeader, new MockHttpServletResponse()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.AUTHORIZATION_HEADER_MISSING));
    }

    @Test
    void expiredToken_mapsToJwtExpired() {
        when(jwtService.parseClaims(TOKEN)).thenThrow(mock(ExpiredJwtException.class));

        assertThatThrownBy(() -> service.refreshToken(request, new MockHttpServletResponse()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_EXPIRED));
    }
}
