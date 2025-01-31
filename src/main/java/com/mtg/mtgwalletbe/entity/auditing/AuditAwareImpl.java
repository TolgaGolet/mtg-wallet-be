package com.mtg.mtgwalletbe.entity.auditing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class AuditAwareImpl implements AuditorAware<String> {
    private static final String ANONYMOUS_USER = "anonymousUser";
    public static final String SYSTEM_USER = "SYSTEM";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Authentication is null or not authenticated");
            return Optional.of(SYSTEM_USER);
        }
        return !Objects.equals(authentication.getName(), ANONYMOUS_USER) ? Optional.ofNullable(authentication.getName()) : Optional.of(SYSTEM_USER);
    }
}
