package com.mtg.mtgwalletbe.entity.auditing;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class AuditAwareImpl implements AuditorAware<String> {
    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String SYSTEM_USER = "SYSTEM";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return !Objects.equals(authentication.getName(), ANONYMOUS_USER) ? Optional.ofNullable(authentication.getName()) : Optional.of(SYSTEM_USER);
    }
}
