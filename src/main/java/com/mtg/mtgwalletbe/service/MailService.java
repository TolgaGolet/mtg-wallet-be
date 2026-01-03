package com.mtg.mtgwalletbe.service;

public interface MailService {
    void sendSimple(String to, String subject, String text);

    void send(String to, String subject, String text);
}
