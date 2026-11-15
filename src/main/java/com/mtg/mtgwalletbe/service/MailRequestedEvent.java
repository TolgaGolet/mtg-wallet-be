package com.mtg.mtgwalletbe.service;

/**
 * Published inside a transaction to send an email once it commits (MailEventListener). The html body may contain
 * token links, so it is never logged.
 */
public record MailRequestedEvent(String to, String subject, String html) {
    @Override
    public String toString() {
        return "MailRequestedEvent[subject=" + subject + "]";
    }
}
