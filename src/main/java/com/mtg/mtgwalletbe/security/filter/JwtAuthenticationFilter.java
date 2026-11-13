package com.mtg.mtgwalletbe.security.filter;

import com.mtg.mtgwalletbe.enums.TokenType;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import com.mtg.mtgwalletbe.security.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.mtg.mtgwalletbe.security.SecurityParams.AUTH_WHITELIST;
import static com.mtg.mtgwalletbe.security.SecurityParams.BEARER_PREFIX;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Profile("!disabled-security")
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Same pattern semantics as Spring Security's matchers, so entries like /auth/totp/* match here too
    private static final List<PathPattern> WHITELIST_PATTERNS = Arrays.stream(AUTH_WHITELIST)
            .map(PathPatternParser.defaultInstance::parse)
            .toList();

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserTokenRepository userTokenRepository;

    static boolean isWhitelisted(String servletPath) {
        PathContainer path = PathContainer.parsePath(servletPath);
        return WHITELIST_PATTERNS.stream().anyMatch(pattern -> pattern.matches(path));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (isWhitelisted(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }
        final String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        final String jwt = authHeader.substring(BEARER_PREFIX.length());
        final Claims claims = jwtService.parseClaims(jwt); // parsed once, reused below
        final String username = claims.getSubject();
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            var isTokenValid = userTokenRepository.findByToken(jwt)
                    .map(t -> t.isActiveAs(TokenType.BEARER)) // refresh tokens are stored too, but aren't access tokens
                    .orElse(false);
            if (jwtService.isTokenValid(claims, userDetails) && isTokenValid) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
