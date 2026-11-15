package com.mtg.mtgwalletbe.service;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.util.backoff.BackOffExecution;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Regression for F4: failed emails were swallowed (no retry) and @Loggable recorded them as successes
class MailServiceImplTest {

    private final MailjetClient client = mock(MailjetClient.class);
    // Same retry count as production, without the waits
    private final MailServiceImpl service = new MailServiceImpl(client, "noreply@example.com", "MTG Wallet",
            "https://app.example", RetryPolicy.builder().maxRetries(2).delay(Duration.ZERO).build());

    private static MailjetResponse response(int status) {
        MailjetResponse response = mock(MailjetResponse.class);
        when(response.getStatus()).thenReturn(status);
        return response;
    }

    @Test
    void firstAttemptSucceeds_noRetry() throws Exception {
        MailjetResponse ok = response(200);
        when(client.post(any(MailjetRequest.class))).thenReturn(ok);

        service.send("a@example.com", "Subject", "<p>hi</p>");

        verify(client, times(1)).post(any(MailjetRequest.class));
    }

    @Test
    void transientFailures_areRetriedWithBackoff() throws Exception {
        MailjetResponse error = response(500);
        MailjetResponse ok = response(200);
        when(client.post(any(MailjetRequest.class)))
                .thenThrow(new RuntimeException("connection reset"))
                .thenReturn(error)
                .thenReturn(ok);

        service.send("a@example.com", "Subject", "<p>hi</p>");

        verify(client, times(3)).post(any(MailjetRequest.class));
    }

    @Test
    void productionPolicy_waitsOneThenFourSeconds() {
        BackOffExecution backOff = MailServiceImpl.RETRY_POLICY.getBackOff().start();

        assertThat(List.of(backOff.nextBackOff(), backOff.nextBackOff(), backOff.nextBackOff()))
                .containsExactly(1_000L, 4_000L, BackOffExecution.STOP);
    }

    @Test
    void persistentFailure_throwsAfterTheLastAttempt() throws Exception {
        MailjetResponse error = response(503);
        when(client.post(any(MailjetRequest.class))).thenReturn(error);

        assertThatThrownBy(() -> service.send("a@example.com", "Subject", "hi"))
                .isInstanceOf(MailServiceImpl.MailDeliveryException.class)
                .hasMessageContaining("503")
                .hasMessageNotContaining("a@example.com"); // no recipient in the message (it ends up in logs)
        verify(client, times(3)).post(any(MailjetRequest.class));
    }

    @Test
    void listener_sendsTheRequestedMail() {
        MailService mailService = mock(MailService.class);

        new MailEventListener(mailService).onMailRequested(new MailRequestedEvent("a@example.com", "Subject", "<p>hi</p>"));

        verify(mailService).send("a@example.com", "Subject", "<p>hi</p>");
    }
}
