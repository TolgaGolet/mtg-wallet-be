package com.mtg.mtgwalletbe.security.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiting per client IP (in-memory, per instance).
 * <p>
 * The client IP is {@code request.getRemoteAddr()}. With {@code server.forward-headers-strategy=native},
 * Tomcat sets it from X-Forwarded-For only when the request comes from a trusted (private-range) proxy,
 * so clients can't pick their own key by spoofing the header.
 */
@Profile("!disabled-security")
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    static final long MAX_TRACKED_CLIENTS = 100_000;

    private final Cache<String, AtomicInteger> requestCountsPerIpAddress;
    private final int rateLimitingMaxRequestsPerDuration;

    public RateLimitingFilter(@Value("${mtgWallet.security.rateLimitingMaxRequestsPerDuration}") int rateLimitingMaxRequestsPerDuration,
                              @Value("${mtgWallet.security.rateLimitingDurationInSeconds}") long rateLimitingDurationInSeconds) {
        super();
        this.rateLimitingMaxRequestsPerDuration = rateLimitingMaxRequestsPerDuration;
        // expireAfterWrite: the window starts at the client's first request; counter updates don't re-write the entry
        this.requestCountsPerIpAddress = Caffeine.newBuilder()
                .maximumSize(MAX_TRACKED_CLIENTS)
                .expireAfterWrite(rateLimitingDurationInSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws IOException, ServletException {
        if (isRateLimitExceeded(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitExceeded(String clientIpAddress) {
        AtomicInteger counter = requestCountsPerIpAddress.get(clientIpAddress, key -> new AtomicInteger());
        return counter.incrementAndGet() > rateLimitingMaxRequestsPerDuration;
    }
}
