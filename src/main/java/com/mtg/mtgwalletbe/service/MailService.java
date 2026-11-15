package com.mtg.mtgwalletbe.service;

public interface MailService {
    void send(String to, String subject, String text);
}
