package com.mtg.mtgwalletbe.service;

import jakarta.mail.MessagingException;

public interface MailService {
    void sendSimple(String to, String subject, String text);

    void send(String to, String subject, String text) throws MessagingException;
}
