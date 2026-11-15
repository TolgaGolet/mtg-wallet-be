package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.PasswordResetToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.PasswordResetTokenRepository;
import com.mtg.mtgwalletbe.service.MailRequestedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// F10: the shared one-time-token flow (used by email verification, password reset and account recovery)
class OneTimeTokenServiceTest {

    private final PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final OneTimeTokenService service = new OneTimeTokenService(publisher);
    private final WalletUser user = WalletUser.builder().id(1L).email("a@example.com").build();

    private static PasswordResetToken token(String value, LocalDateTime expiry) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(value);
        token.setExpiryDate(expiry);
        return token;
    }

    @Test
    void issue_replacesExistingTokens_andSavesANewOneWithExpiry() {
        PasswordResetToken old = token("old", LocalDateTime.now().plusMinutes(5));
        when(repository.findAllByUser(user)).thenReturn(List.of(old));
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);

        String value = service.issue(repository, PasswordResetToken::new, user, Duration.ofMinutes(15));

        var order = inOrder(repository);
        order.verify(repository).deleteAll(List.of(old));
        order.verify(repository).flush(); // user_id is unique: the old row must be gone before the insert
        order.verify(repository).save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo(value).isNotEqualTo("old").hasSize(36);
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getExpiryDate()).isCloseTo(LocalDateTime.now().plusMinutes(15), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void issue_withoutExistingToken_doesNotDelete() {
        when(repository.findAllByUser(user)).thenReturn(List.of());

        service.issue(repository, PasswordResetToken::new, user, Duration.ofMinutes(15));

        verify(repository, never()).deleteAll(any());
        verify(repository).save(any(PasswordResetToken.class));
    }

    @Test
    void consume_validToken_isDeleted_singleUse() throws Exception {
        PasswordResetToken stored = token("t", LocalDateTime.now().plusMinutes(1));
        when(repository.findByToken("t")).thenReturn(Optional.of(stored));

        PasswordResetToken result = service.consume(repository, "t",
                GenericExceptionMessages.INVALID_PASSWORD_RESET_TOKEN, GenericExceptionMessages.EXPIRED_PASSWORD_RESET_TOKEN);

        assertThat(result).isSameAs(stored);
        verify(repository).delete(stored);
    }

    @Test
    void consume_unknownToken_isInvalid() {
        when(repository.findByToken("t")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume(repository, "t",
                GenericExceptionMessages.INVALID_PASSWORD_RESET_TOKEN, GenericExceptionMessages.EXPIRED_PASSWORD_RESET_TOKEN))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.INVALID_PASSWORD_RESET_TOKEN);
    }

    @Test
    void consume_expiredToken_isRejected_andNotDeleted() {
        PasswordResetToken stored = token("t", LocalDateTime.now().minusSeconds(1));
        when(repository.findByToken("t")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.consume(repository, "t",
                GenericExceptionMessages.INVALID_PASSWORD_RESET_TOKEN, GenericExceptionMessages.EXPIRED_PASSWORD_RESET_TOKEN))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.EXPIRED_PASSWORD_RESET_TOKEN);
        verify(repository, never()).delete(any());
    }

    // F4: the email is an event handled after commit, not a direct (async) send inside the transaction
    @Test
    void sendAfterCommit_publishesEvent() {
        service.sendAfterCommit("a@example.com", "Subject", "<p>body</p>");

        verify(publisher).publishEvent(new MailRequestedEvent("a@example.com", "Subject", "<p>body</p>"));
    }
}
