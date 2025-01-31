package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.PasswordResetToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.PasswordResetTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final WalletUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${mtgWallet.frontend.url}")
    private String frontendUrl;

    private static final int TOKEN_EXPIRATION_MINUTES = 15;

    @Transactional
    public void initiatePasswordReset(String email) throws MessagingException {
        Optional<WalletUser> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            passwordResetTokenRepository.findByUser(user.get()).ifPresent(passwordResetTokenRepository::delete);
            passwordResetTokenRepository.flush();
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user.get())
                    .expiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES))
                    .build();

            passwordResetTokenRepository.save(resetToken);
            sendPasswordResetEmail(email, token);
        }
    }

    private void sendPasswordResetEmail(String email, String token) throws MessagingException {
        String resetUrl = frontendUrl + "/reset-password/" + token;
        String emailContent = String.format("""
                <p>You have requested to reset your password. Please click the link below to proceed:</p>
                <p><a href="%s">Reset Password</a></p>
                <p>This link will expire in %d minutes.</p>
                <p>If you did not request this password reset, please ignore this email.</p>
                """, resetUrl, TOKEN_EXPIRATION_MINUTES);

        mailService.send(email, "Password Reset Request", emailContent);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) throws MtgWalletGenericException {
        PasswordResetToken resetToken = validateAndGetToken(token);

        WalletUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    private PasswordResetToken validateAndGetToken(String token) throws MtgWalletGenericException {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.INVALID_PASSWORD_RESET_TOKEN.getMessage()));

        if (resetToken.isExpired()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.EXPIRED_PASSWORD_RESET_TOKEN.getMessage());
        }

        return resetToken;
    }
}
