package com.mtg.mtgwalletbe.service;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import com.mtg.mtgwalletbe.annotation.Loggable;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailServiceImpl implements MailService {
    private final MailjetClient mailjetClient;
    private final String senderEmail;
    private final String senderName;
    private final String frontendUrl;

    public MailServiceImpl(
            @Value("${mtgWallet.mailjet.apiKey}") String apiKey,
            @Value("${mtgWallet.mailjet.secretKey}") String secretKey,
            @Value("${mtgWallet.mailjet.senderEmail}") String senderEmail,
            @Value("${mtgWallet.mailjet.senderName}") String senderName,
            @Value("${mtgWallet.frontend.url}") String frontendUrl) {
        this.mailjetClient = new MailjetClient(
                ClientOptions.builder()
                        .apiKey(apiKey)
                        .apiSecretKey(secretKey)
                        .build());
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Loggable
    @Async
    public void send(String to, String subject, String text) {
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
                            .header a {
                                color: white;
                                text-decoration: none;
                            }
                            .header a:hover {
                                text-decoration: underline;
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
                                <h1><a href="%s">MTG Wallet</a></h1>
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
                """.formatted(frontendUrl, subject, text);

        sendEmail(to, subject, null, htmlContent);
    }

    @Override
    @Loggable
    @Async
    public void sendSimple(String to, String subject, String text) {
        sendEmail(to, subject, text, null);
    }

    private void sendEmail(String to, String subject, String textContent, String htmlContent) {
        try {
            JSONObject message = new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                            .put("Email", senderEmail)
                            .put("Name", senderName))
                    .put(Emailv31.Message.TO, new JSONArray()
                            .put(new JSONObject()
                                    .put("Email", to)))
                    .put(Emailv31.Message.SUBJECT, subject);

            if (textContent != null) {
                message.put(Emailv31.Message.TEXTPART, textContent);
            }
            if (htmlContent != null) {
                message.put(Emailv31.Message.HTMLPART, htmlContent);
            }

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray().put(message));

            MailjetResponse response = mailjetClient.post(request);

            if (response.getStatus() != 200) {
                log.error("Failed to send email to {}. Status: {}, Data: {}",
                        to, response.getStatus(), response.getData());
            } else {
                log.info("Email sent successfully to {}", to);
            }
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage(), e);
        }
    }
}
