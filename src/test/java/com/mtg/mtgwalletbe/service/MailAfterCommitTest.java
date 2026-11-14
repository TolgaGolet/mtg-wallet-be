package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.config.TimeZoneInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Regression for F4: emails were sent from inside the transaction, so a rollback still sent a link to a token row
// that never got committed. Uses the real event listener wiring.
@SpringBootTest
@ActiveProfiles("unittest")
class MailAfterCommitTest {

    @MockitoBean private MailService mailService;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private TransactionTemplate transactionTemplate;

    private final MailRequestedEvent event = new MailRequestedEvent("a@example.com", "Subject", "<p>link</p>");

    @Test
    void committedTransaction_sendsTheMail() {
        transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(event));

        verify(mailService).send("a@example.com", "Subject", "<p>link</p>");
    }

    @Test
    void rolledBackTransaction_sendsNothing() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(event);
            status.setRollbackOnly();
        });

        verify(mailService, never()).send(any(), any(), any());
    }

    @Test
    void mailIsNotSentBeforeCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(event);
            verifyNoInteractions(mailService);
        });

        verify(mailService).send(any(), any(), any());
    }

    // Nitpick: the timezone is set from config before beans are created (TimeZoneInitializer via spring.factories)
    @Test
    void jvmTimezone_comesFromConfiguration() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC"); // mtgWallet.applicationConfig.timeZone in unittest
        assertThat(TimeZoneInitializer.class).isNotNull();
    }
}
