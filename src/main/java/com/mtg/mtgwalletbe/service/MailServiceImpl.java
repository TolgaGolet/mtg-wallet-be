package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.annotation.Loggable;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private final JavaMailSender emailSender;

    @Override
    @Loggable
    @Async
    public void send(String to, String subject, String text) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            .email-container {
                                max-width: 600px;
                                margin: 0 auto;
                                font-family: 'Arial', sans-serif;
                                line-height: 1.6;
                                color: #333333;
                            }
                            .header {
                                background-color: #4e565e;
                                padding: 20px;
                                color: white;
                                text-align: center;
                                border-radius: 5px 5px 0 0;
                            }
                            .content {
                                padding: 20px;
                                background-color: #ffffff;
                                border: 1px solid #e0e0e0;
                            }
                            .footer {
                                text-align: center;
                                padding: 20px;
                                font-size: 12px;
                                color: #666666;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="email-container">
                            <div class="header">
                                <h1>MTG Wallet</h1>
                            </div>
                            <div class="content">
                                <h2>
                                    %s
                                </h2>
                                %s
                            </div>
                            <div class="footer">
                                This message was sent automatically by MTG Wallet.
                            </div>
                        </div>
                    </body>
                </html>
                """.formatted(subject, text);

        helper.setText(htmlContent, true);
        emailSender.send(message);
    }

    @Override
    @Loggable
    @Async
    public void sendSimple(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}
