package com.mtg.mtgwalletbe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends requested emails only after the publishing transaction commits, so a rolled-back registration or token
 * never produces a link to a row that doesn't exist. Without a transaction the mail is sent right away.
 */
@Component
@RequiredArgsConstructor
public class MailEventListener {
    private final MailService mailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMailRequested(MailRequestedEvent event) {
        mailService.send(event.to(), event.subject(), event.html()); // @Async: doesn't delay the response
    }
}
