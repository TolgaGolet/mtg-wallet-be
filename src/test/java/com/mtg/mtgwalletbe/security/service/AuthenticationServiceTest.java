package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.TokenType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.security.api.request.AuthenticationRequest;
import com.mtg.mtgwalletbe.security.api.request.RegisterRequest;
import com.mtg.mtgwalletbe.security.api.request.TotpDisableRequest;
import com.mtg.mtgwalletbe.security.api.request.TotpVerificationRequest;
import com.mtg.mtgwalletbe.security.api.response.AuthenticationResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
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
    @Mock private com.mtg.mtgwalletbe.service.UserService userService;
    @Mock private com.mtg.mtgwalletbe.service.DefaultDataService defaultDataService;
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

        AuthenticationResponse response = service.refreshToken(request);

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

        assertThatThrownBy(() -> service.refreshToken(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    @Test
    void refresh_withRevokedRefreshToken_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.REFRESH, true)));

        assertThatThrownBy(() -> service.refreshToken(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
        verify(jwtService, never()).generateToken(any());
    }

    // Regression: the user came from the token's username, which a deleted account frees for someone else
    @Test
    void refresh_issuesTokensForTheTokenOwner_notByUsernameLookup() throws Exception {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.REFRESH, false)));
        when(walletUserRepository.findByUsername("alice")).thenReturn(Optional.of(WalletUser.builder().id(2L).username("alice").build()));

        service.refreshToken(request);

        verify(jwtService).generateToken(user);
        verify(walletUserRepository, never()).findByUsername(any());
    }

    @Test
    void refresh_withAccessTokenByTypeClaim_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.ACCESS_TOKEN_TYPE);

        assertThatThrownBy(() -> service.refreshToken(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    // Tokens issued before the "typ" claim existed: access tokens are recognised by their UserToken row
    @Test
    void refresh_withLegacyAccessTokenWithoutTypeClaim_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(null);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.BEARER, false)));

        assertThatThrownBy(() -> service.refreshToken(request))
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

        assertThatThrownBy(() -> service.refreshToken(noHeader))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.AUTHORIZATION_HEADER_MISSING));
    }

    @Test
    void expiredToken_mapsToJwtExpired() {
        when(jwtService.parseClaims(TOKEN)).thenThrow(mock(ExpiredJwtException.class));

        assertThatThrownBy(() -> service.refreshToken(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_EXPIRED));
    }

    // Regression: accounts verified before verification created the defaults had no way left to get them
    @Test
    void login_createsMissingDefaults() throws Exception {
        user.setIsEmailVerified(Boolean.TRUE);

        service.authenticateUser("alice", "secret");

        verify(defaultDataService).createDefaultsFor(user);
    }

    @Test
    void login_withUnverifiedEmail_createsNoDefaults() {
        user.setIsEmailVerified(Boolean.FALSE);

        assertThatThrownBy(() -> service.authenticateUser("alice", "secret")).isInstanceOf(MtgWalletGenericException.class);

        verifyNoInteractions(defaultDataService);
    }

    // --- register ---

    private static RegisterRequest registerRequest(String username) {
        return RegisterRequest.builder().username(username).email(username + "@example.com")
                .name("Name").surname("Surname").password("plain-password").build();
    }

    @Test
    void register_savesUnverifiedUserWithHashedPassword_andSendsVerification() throws Exception {
        when(walletUserRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(walletUserRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed");
        when(walletUserRepository.save(any(WalletUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthenticationResponse response = service.register(registerRequest("bob"));

        ArgumentCaptor<WalletUser> saved = ArgumentCaptor.forClass(WalletUser.class);
        verify(walletUserRepository).save(saved.capture());
        WalletUser newUser = saved.getValue();
        assertThat(newUser.getPassword()).isEqualTo("hashed");
        assertThat(newUser.getIsEmailVerified()).isFalse();
        assertThat(newUser.getTotpEnabled()).isFalse();
        assertThat(newUser.getIsDefaultsCreated()).isFalse();
        verify(emailVerificationService).createVerificationToken(newUser);
        assertThat(response.isEmailVerificationRequired()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isNull();
    }

    @Test
    void register_existingUsername_isRejected() {
        assertThatThrownBy(() -> service.register(registerRequest("alice")))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.USERNAME_ALREADY_EXISTS));
        verify(walletUserRepository, never()).save(any());
    }

    @Test
    void register_existingEmail_isRejected() {
        when(walletUserRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(walletUserRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerRequest("bob")))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.EMAIL_ALREADY_EXISTS));
        verify(walletUserRepository, never()).save(any());
    }

    // The auditing user name must never belong to a real account, in any letter case
    @ParameterizedTest
    @ValueSource(strings = {"system", "SYSTEM", "System", "sYsTeM"})
    void register_systemUsernameInAnyCase_isRejected(String username) {
        when(walletUserRepository.findByUsername(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(registerRequest(username)))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.SYSTEM_USERNAME_NOT_ALLOWED));
        verify(walletUserRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_badCredentials_mapsToBadUsernameOrPassword() {
        when(authenticationManager.authenticate(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.authenticate(AuthenticationRequest.builder().username("alice").password("wrong").build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_withoutTotp_revokesOldSessions_andIssuesBothTokens() throws Exception {
        user.setIsEmailVerified(Boolean.TRUE);
        user.setTotpEnabled(Boolean.FALSE);

        AuthenticationResponse response = service.authenticate(AuthenticationRequest.builder().username("alice").password("secret").build());

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(response.isTotpRequired()).isFalse();
        var order = inOrder(userService, userTokenRepository);
        order.verify(userService).revokeAllSessions(1L);
        order.verify(userTokenRepository, times(2)).save(any(UserToken.class));
        order.verify(userTokenRepository).deleteOlderTokensOfUserByCount(1L, 5);
    }

    @Test
    void login_withTotpEnabled_asksForTheCode_withoutIssuingTokens() throws Exception {
        user.setIsEmailVerified(Boolean.TRUE);
        user.setTotpEnabled(Boolean.TRUE);

        AuthenticationResponse response = service.authenticate(AuthenticationRequest.builder().username("alice").password("secret").build());

        assertThat(response.isTotpRequired()).isTrue();
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        verify(jwtService, never()).generateToken(any());
        verify(userTokenRepository, never()).save(any());
    }

    // --- TOTP ---

    private void activeAccessToken() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.ACCESS_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.BEARER, false)));
    }

    @Test
    void totpSetup_storesNewSecretOnTheTokenOwner_andReturnsQrCode() throws Exception {
        activeAccessToken();
        when(totpService.generateSecret()).thenReturn("SECRET");
        when(totpService.getQrCodeImageUri("SECRET", "alice")).thenReturn("qr-base64");

        var response = service.setupTotp(request);

        assertThat(response.getSecret()).isEqualTo("SECRET");
        assertThat(response.getQrCodeImage()).isEqualTo("qr-base64");
        assertThat(user.getTotpSecret()).isEqualTo("SECRET");
        verify(walletUserRepository).save(user);
    }

    @Test
    void totpEnable_withValidCode_enablesTotp_andIssuesFreshTokens() throws Exception {
        activeAccessToken();
        user.setTotpSecret("SECRET");
        when(totpService.verifyCode("123456", "SECRET")).thenReturn(true);

        AuthenticationResponse response = service.verifyAndEnableTotp(request, "123456");

        assertThat(user.getTotpEnabled()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("new-access");
        verify(userService).revokeAllSessions(1L);
    }

    @Test
    void totpEnable_withWrongCode_isRejected_andLeavesTotpOff() {
        activeAccessToken();
        user.setTotpSecret("SECRET");
        user.setTotpEnabled(false);
        when(totpService.verifyCode("000000", "SECRET")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyAndEnableTotp(request, "000000"))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.INVALID_TOTP_CODE));
        assertThat(user.getTotpEnabled()).isFalse();
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void totpLogin_withValidCode_issuesTokens() throws Exception {
        user.setIsEmailVerified(Boolean.TRUE);
        user.setTotpSecret("SECRET");
        when(totpService.verifyCode("123456", "SECRET")).thenReturn(true);

        AuthenticationResponse response = service.verifyTotp(TotpVerificationRequest.builder()
                .username("alice").password("secret").verificationCode("123456").build());

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void totpLogin_withWrongCode_isRejected_withoutTokens() {
        user.setIsEmailVerified(Boolean.TRUE);
        user.setTotpSecret("SECRET");
        when(totpService.verifyCode("000000", "SECRET")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyTotp(TotpVerificationRequest.builder()
                .username("alice").password("secret").verificationCode("000000").build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.INVALID_TOTP_CODE));
        verify(jwtService, never()).generateToken(any());
    }

    // The password is checked before the code, so a code alone never logs anyone in
    @Test
    void totpLogin_withWrongPassword_isRejectedBeforeCheckingTheCode() {
        when(authenticationManager.authenticate(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.verifyTotp(TotpVerificationRequest.builder()
                .username("alice").password("wrong").verificationCode("123456").build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD));
        verifyNoInteractions(totpService);
    }

    @Test
    void totpDisable_withValidCode_turnsTotpOff() throws Exception {
        activeAccessToken();
        user.setTotpSecret("SECRET");
        user.setTotpEnabled(true);
        when(totpService.verifyCode("123456", "SECRET")).thenReturn(true);

        service.disableTotp(request, TotpDisableRequest.builder().code("123456").build());

        assertThat(user.getTotpEnabled()).isFalse();
        verify(walletUserRepository).save(user);
    }

    @Test
    void totpDisable_withWrongCode_keepsTotpOn() {
        activeAccessToken();
        user.setTotpSecret("SECRET");
        user.setTotpEnabled(true);
        when(totpService.verifyCode("000000", "SECRET")).thenReturn(false);

        assertThatThrownBy(() -> service.disableTotp(request, TotpDisableRequest.builder().code("000000").build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.INVALID_TOTP_CODE));
        assertThat(user.getTotpEnabled()).isTrue();
        verify(walletUserRepository, never()).save(any());
    }

    // --- bearer token edge cases ---

    @Test
    void headerWithoutBearerPrefix_isRejected() {
        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic " + TOKEN);

        assertThatThrownBy(() -> service.refreshToken(basic))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.AUTHORIZATION_HEADER_MISSING));
        verify(jwtService, never()).parseClaims(any());
    }

    @Test
    void malformedToken_mapsToJwtNotValid() {
        when(jwtService.parseClaims(TOKEN)).thenThrow(new io.jsonwebtoken.MalformedJwtException("garbage"));

        assertThatThrownBy(() -> service.refreshToken(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
    }

    @Test
    void tokenWithoutSubject_isRejected() {
        when(claims.getSubject()).thenReturn(null);

        assertThatThrownBy(() -> service.refreshToken(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_SUBJECT_MISSING));
    }

    // e.g. the row's owner was renamed / anonymized since the token was issued
    @Test
    void storedTokenWhoseClaimsDontMatchItsOwner_isRejected() {
        when(claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class)).thenReturn(JwtService.REFRESH_TOKEN_TYPE);
        when(userTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(storedToken(TokenType.REFRESH, false)));
        when(jwtService.isTokenValid(claims, user)).thenReturn(false);

        assertThatThrownBy(() -> service.refreshToken(request))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.JWT_NOT_VALID));
        verify(jwtService, never()).generateToken(any());
    }
}
