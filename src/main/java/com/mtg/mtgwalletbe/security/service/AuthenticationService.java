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
import com.mtg.mtgwalletbe.security.api.response.AuthenticationResponse;
import io.jsonwebtoken.ExpiredJwtException;
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

import static com.mtg.mtgwalletbe.security.SecurityParams.BEARER_PREFIX;

@Profile("!disabled-security")
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final WalletUserRepository walletUserRepository;
    private final UserTokenRepository userTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private static final Integer MAX_USER_TOKEN_COUNT = 5;

    public AuthenticationResponse register(RegisterRequest request) throws MtgWalletGenericException {
        if (walletUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.USERNAME_ALREADY_EXISTS.getMessage());
        }
        var user = WalletUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .name(request.getName())
                .surname(request.getSurname())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        var savedUser = walletUserRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(savedUser, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws MtgWalletGenericException {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new MtgWalletGenericException(GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD.getMessage());
        }
        var user = walletUserRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);
        userTokenRepository.deleteOlderTokensOfUserByCount(user.getId(), MAX_USER_TOKEN_COUNT);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void saveUserToken(WalletUser user, String jwtToken) {
        var token = UserToken.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        userTokenRepository.save(token);
    }

    private void revokeAllUserTokens(WalletUser user) {
        var validUserTokens = userTokenRepository.findAllValidUserTokensByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        userTokenRepository.saveAll(validUserTokens);
    }

    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws MtgWalletGenericException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String username;
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.AUTHORIZATION_HEADER_MISSING.getMessage());
        }
        refreshToken = authHeader.substring(7);
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_EXPIRED.getMessage());
        }
        if (username == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_SUBJECT_MISSING.getMessage());
        }
        var user = walletUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.JWT_NOT_VALID.getMessage());
        }
        var accessToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);
        userTokenRepository.deleteOlderTokensOfUserByCount(user.getId(), MAX_USER_TOKEN_COUNT);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
