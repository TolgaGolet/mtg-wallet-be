package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.PasswordResetToken;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.PasswordResetTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final WalletUserRepository userRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final UserService userService;

    @Value("${mtgWallet.frontend.url}")
    private String frontendUrl;

    private static final int TOKEN_EXPIRATION_MINUTES = 15;

    /**
     * Silent for unknown emails, so the endpoint doesn't reveal which addresses are registered.
     */
    public void initiatePasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = oneTimeTokenService.issue(passwordResetTokenRepository, PasswordResetToken::new,
                    user, Duration.ofMinutes(TOKEN_EXPIRATION_MINUTES));
            String resetUrl = frontendUrl + "/reset-password/" + token;
            String emailContent = String.format("""
                    <p>You have requested to reset your password. Please click the link below to proceed:</p>
                    <p><a href="%s">Reset Password</a></p>
                    <p>This link will expire in %d minutes.</p>
                    <p>If you did not request this password reset, please ignore this email.</p>
                    """, resetUrl, TOKEN_EXPIRATION_MINUTES);
            oneTimeTokenService.sendAfterCommit(email, "Password Reset Request", emailContent);
        });
    }

    public void resetPassword(String token, String newPassword) throws MtgWalletGenericException {
        PasswordResetToken resetToken = oneTimeTokenService.consume(passwordResetTokenRepository, token,
                GenericExceptionMessages.INVALID_PASSWORD_RESET_TOKEN, GenericExceptionMessages.EXPIRED_PASSWORD_RESET_TOKEN);
        userService.replacePassword(resetToken.getUser(), newPassword); // someone else may be logged in
    }
}
