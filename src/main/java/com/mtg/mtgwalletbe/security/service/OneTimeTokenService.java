package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.OneTimeToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.OneTimeTokenRepository;
import com.mtg.mtgwalletbe.service.MailRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The single flow behind the emailed one-time tokens: one live token per user, single use, expiry checked on use,
 * and the email goes out only after the transaction commits.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OneTimeTokenService {
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Replaces any token the user already has with a new random one valid for ttl; returns the token value.
     */
    public <T extends OneTimeToken> String issue(OneTimeTokenRepository<T> repository, Supplier<T> factory, WalletUser user, Duration ttl) {
        List<T> existing = repository.findAllByUser(user);
        if (!existing.isEmpty()) {
            repository.deleteAll(existing);
            repository.flush(); // the user_id column is unique: delete before inserting the new row
        }
        T token = factory.get();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plus(ttl));
        repository.save(token);
        return token.getToken();
    }

    /**
     * Looks the token up, rejects it when unknown or expired, and deletes it (single use). The caller then acts on
     * the returned token's user within the same transaction.
     */
    public <T extends OneTimeToken> T consume(OneTimeTokenRepository<T> repository, String token,
                                              GenericExceptionMessages invalid, GenericExceptionMessages expired) throws MtgWalletGenericException {
        T stored = repository.findByToken(token).orElseThrow(() -> new MtgWalletGenericException(invalid));
        if (stored.isExpired()) {
            throw new MtgWalletGenericException(expired);
        }
        repository.delete(stored);
        return stored;
    }

    /**
     * Queues the email; it is sent after the surrounding transaction commits.
     */
    public void sendAfterCommit(String to, String subject, String html) {
        eventPublisher.publishEvent(new MailRequestedEvent(to, subject, html));
    }
}
