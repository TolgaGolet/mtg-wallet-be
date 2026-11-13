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
import com.mtg.mtgwalletbe.security.api.response.TotpSetupResponse;
import dev.samstevens.totp.exceptions.QrGenerationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.mtg.mtgwalletbe.entity.auditing.AuditAwareImpl.SYSTEM_USER;
import static com.mtg.mtgwalletbe.security.SecurityParams.BEARER_PREFIX;

@Profile("!disabled-security")
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final WalletUserRepository walletUserRepository;
    private final UserTokenRepository userTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final TotpService totpService;
    private static final Integer MAX_USER_TOKEN_COUNT = 5;

    public AuthenticationResponse register(RegisterRequest request) throws MtgWalletGenericException {
        if (walletUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.USERNAME_ALREADY_EXISTS);
        }
        if (walletUserRepository.existsByEmail(request.getEmail())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.EMAIL_ALREADY_EXISTS);
        }
        if (request.getUsername().equalsIgnoreCase(SYSTEM_USER)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.SYSTEM_USERNAME_NOT_ALLOWED);
        }
        var user = WalletUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .name(request.getName())
                .surname(request.getSurname())
                .password(passwordEncoder.encode(request.getPassword()))
                .isDefaultsCreated(Boolean.FALSE)
                .isEmailVerified(Boolean.FALSE)
                .totpEnabled(Boolean.FALSE)
                .build();
        var savedUser = walletUserRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        saveUserToken(savedUser, jwtToken, TokenType.BEARER);
        emailVerificationService.createVerificationToken(savedUser);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .emailVerificationRequired(true)
                .build();
    }

    public WalletUser authenticateUser(String username, String password) throws MtgWalletGenericException {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            password
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new MtgWalletGenericException(GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD);
        }
        WalletUser user = walletUserRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        if (Boolean.FALSE.equals(user.getIsEmailVerified())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.NOT_VERIFIED_EMAIL);
        }
        return user;
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws MtgWalletGenericException {
        WalletUser user = authenticateUser(request.getUsername(), request.getPassword());
        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            return AuthenticationResponse.builder()
                    .totpRequired(true)
                    .build();
        }
        return generateTokensAndBuildResponse(user);
    }

    private AuthenticationResponse generateTokensAndBuildResponse(WalletUser user) {
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken, TokenType.BEARER);
        saveUserToken(user, refreshToken, TokenType.REFRESH);
        userTokenRepository.deleteOlderTokensOfUserByCount(user.getId(), MAX_USER_TOKEN_COUNT);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void saveUserToken(WalletUser user, String jwtToken, TokenType tokenType) {
        var token = UserToken.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(tokenType)
                .expired(false)
                .revoked(false)
                .build();
        userTokenRepository.save(token);
    }

    private void revokeAllUserTokens(WalletUser user) {
        var validUserTokens = userTokenRepository.findAllNotFullyRevokedTokensByUserId(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(UserToken::revoke);
        userTokenRepository.saveAll(validUserTokens);
    }

    @Transactional
    public TotpSetupResponse setupTotp(HttpServletRequest request) throws QrGenerationException, MtgWalletGenericException {
        var user = resolveUserFromBearer(request, TokenKind.ACCESS);

        String secret = totpService.generateSecret();
        String qrCodeImage = totpService.getQrCodeImageUri(secret, user.getUsername());

        user.setTotpSecret(secret);
        walletUserRepository.save(user);

        return TotpSetupResponse.builder()
                .qrCodeImage(qrCodeImage)
                .secret(secret)
                .build();
    }

    @Transactional
    public AuthenticationResponse verifyAndEnableTotp(HttpServletRequest request, String code) throws MtgWalletGenericException {
        var user = resolveUserFromBearer(request, TokenKind.ACCESS);

        if (!totpService.verifyCode(code, user.getTotpSecret())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.INVALID_TOTP_CODE);
        }

        user.setTotpEnabled(true);
        walletUserRepository.save(user);

        return generateTokensAndBuildResponse(user);
    }

    @Transactional
    public AuthenticationResponse verifyTotp(TotpVerificationRequest request) throws MtgWalletGenericException {
        WalletUser user = authenticateUser(request.getUsername(), request.getPassword());

        if (!totpService.verifyCode(request.getVerificationCode(), user.getTotpSecret())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.INVALID_TOTP_CODE);
        }

        return generateTokensAndBuildResponse(user);
    }

    @Transactional
    public void disableTotp(HttpServletRequest request, TotpDisableRequest totpDisableRequest) throws MtgWalletGenericException {
        var user = resolveUserFromBearer(request, TokenKind.ACCESS);

        if (!totpService.verifyCode(totpDisableRequest.getCode(), user.getTotpSecret())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.INVALID_TOTP_CODE);
        }

        user.setTotpEnabled(false);
        walletUserRepository.save(user);
    }

    @Transactional
    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws MtgWalletGenericException {
        // Rotation: the used refresh token is revoked together with every other token of the user
        return generateTokensAndBuildResponse(resolveUserFromBearer(request, TokenKind.REFRESH));
    }

    private enum TokenKind {ACCESS, REFRESH}

    /**
     * Single place for bearer-token handling on endpoints the JWT filter doesn't authenticate (whitelisted paths).
     * Both kinds must match an active UserToken row of their own type (BEARER for access, the same rule as
     * JwtAuthenticationFilter; REFRESH for refresh), so logout, login, refresh and account deletion revoke them.
     * The user is the row's owner, not a lookup by the token's username, which may since belong to another account.
     */
    private WalletUser resolveUserFromBearer(HttpServletRequest request, TokenKind expected) throws MtgWalletGenericException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.AUTHORIZATION_HEADER_MISSING);
        }
        final String token = authHeader.substring(BEARER_PREFIX.length());
        final Claims claims;
        try {
            claims = jwtService.parseClaims(token); // parsed once for subject, type and expiry
        } catch (ExpiredJwtException e) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_NOT_VALID);
        }
        final String username = claims.getSubject();
        final String tokenType = JwtService.tokenTypeOf(claims);
        if (username == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_SUBJECT_MISSING);
        }
        final TokenType expectedRowType = expected == TokenKind.ACCESS ? TokenType.BEARER : TokenType.REFRESH;
        final String wrongClaimType = expected == TokenKind.ACCESS ? JwtService.REFRESH_TOKEN_TYPE : JwtService.ACCESS_TOKEN_TYPE;
        Optional<UserToken> stored = userTokenRepository.findByToken(token).filter(t -> t.isActiveAs(expectedRowType));
        if (wrongClaimType.equals(tokenType) || stored.isEmpty()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_NOT_VALID);
        }
        WalletUser user = stored.get().getUser();
        if (!jwtService.isTokenValid(claims, user)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_NOT_VALID);
        }
        return user;
    }
}
