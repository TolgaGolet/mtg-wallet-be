package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.EmailVerificationToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.EmailVerificationTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final WalletUserRepository userRepository;
    private final MailService mailService;
    private static final int TOKEN_EXPIRATION_HOURS = 24;

    @Value("${mtgWallet.frontend.url}")
    private String frontendUrl;

    public void createVerificationToken(WalletUser user) throws MessagingException {
        // Delete any existing tokens
        List<EmailVerificationToken> existingTokens = emailVerificationTokenRepository.findAllByUser(user);
        if (!existingTokens.isEmpty()) {
            emailVerificationTokenRepository.deleteAll(existingTokens);
            emailVerificationTokenRepository.flush();
        }

        String token = UUID.randomUUID().toString();
        EmailVerificationToken emailVerificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
                .build();

        emailVerificationTokenRepository.save(emailVerificationToken);
        sendVerificationEmail(user.getEmail(), token);
    }

    private void sendVerificationEmail(String email, String token) throws MessagingException {
        String verificationUrl = frontendUrl + "/verify-email/" + token;
        String emailContent = String.format("""
                <p>Please click the link below to verify your email address:</p>
                <p><a href="%s">Verify Email</a></p>
                <p>This link will expire in %d hours.</p>
                """, verificationUrl, TOKEN_EXPIRATION_HOURS);

        mailService.send(email, "Verify Your Email Address", emailContent);
    }

    public void verifyEmail(String token) throws MtgWalletGenericException {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.INVALID_EMAIL_VERIFICATION_TOKEN.getMessage()));

        if (emailVerificationToken.isExpired()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.EXPIRED_EMAIL_VERIFICATION_TOKEN.getMessage());
        }

        WalletUser user = emailVerificationToken.getUser();
        user.setIsEmailVerified(Boolean.TRUE);

        userRepository.save(user);
        emailVerificationTokenRepository.delete(emailVerificationToken);
    }
}
