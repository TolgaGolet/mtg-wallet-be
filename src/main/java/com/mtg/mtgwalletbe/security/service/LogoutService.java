package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import static com.mtg.mtgwalletbe.security.SecurityParams.BEARER_PREFIX;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {
    private final UserTokenRepository userTokenRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String authHeader = request.getHeader(AUTHORIZATION);
        final String jwt;
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return;
        }
        jwt = authHeader.substring(BEARER_PREFIX.length());
        // Revokes every token of the user, including the refresh token issued alongside this access token
        userTokenRepository.findByToken(jwt).ifPresent(storedToken -> {
            var tokens = userTokenRepository.findAllNotFullyRevokedTokensByUserId(storedToken.getUser().getId());
            tokens.forEach(UserToken::revoke);
            userTokenRepository.saveAll(tokens);
            SecurityContextHolder.clearContext();
        });
    }
}
