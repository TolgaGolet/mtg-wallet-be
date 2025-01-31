package com.mtg.mtgwalletbe.security.filter;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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

@Profile("!disabled-security")
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final LoadingCache<String, Integer> requestCountsPerIpAddress;
    private final int rateLimitingMaxRequestsPerDuration;

    public RateLimitingFilter(@Value("${mtgWallet.security.rateLimitingMaxRequestsPerDuration}") int rateLimitingMaxRequestsPerDuration,
                              @Value("${mtgWallet.security.rateLimitingDurationInSeconds}") long rateLimitingDurationInSeconds) {
        super();
        this.rateLimitingMaxRequestsPerDuration = rateLimitingMaxRequestsPerDuration;
        requestCountsPerIpAddress = Caffeine.newBuilder().
                expireAfterWrite(rateLimitingDurationInSeconds, TimeUnit.SECONDS).build(new CacheLoader<String, Integer>() {
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws IOException, ServletException {
        String clientIpAddress = getClientIP(request);
        if (isMaximumRequestsPerSecondExceeded(clientIpAddress)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isMaximumRequestsPerSecondExceeded(String clientIpAddress) {
        Integer requests = 0;
        requests = requestCountsPerIpAddress.get(clientIpAddress);

        if (requests != null) {
            if (requests >= rateLimitingMaxRequestsPerDuration) {
                requestCountsPerIpAddress.asMap().remove(clientIpAddress);
                requestCountsPerIpAddress.put(clientIpAddress, requests);
                return true;
            }
        } else {
            requests = 0;
        }
        requests++;
        requestCountsPerIpAddress.put(clientIpAddress, requests);
        return false;
    }

    public String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}