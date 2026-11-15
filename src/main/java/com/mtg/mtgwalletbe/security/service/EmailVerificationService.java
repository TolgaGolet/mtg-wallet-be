package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.config.FrontendProperties;
import com.mtg.mtgwalletbe.entity.EmailVerificationToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.EmailVerificationTokenRepository;
import com.mtg.mtgwalletbe.service.DefaultDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final DefaultDataService defaultDataService;
    private final FrontendProperties frontendProperties;
    private static final int TOKEN_EXPIRATION_HOURS = 24;

    public void createVerificationToken(WalletUser user) {
        String token = oneTimeTokenService.issue(emailVerificationTokenRepository, EmailVerificationToken::new,
                user, Duration.ofHours(TOKEN_EXPIRATION_HOURS));
        String verificationUrl = frontendProperties.url() + "/verify-email/" + token;
        String emailContent = String.format("""
                <p>Please click the link below to verify your email address:</p>
                <p><a href="%s">Verify Email</a></p>
                <p>This link will expire in %d hours.</p>
                """, verificationUrl, TOKEN_EXPIRATION_HOURS);
        oneTimeTokenService.sendAfterCommit(user.getEmail(), "Verify Your Email Address", emailContent);
    }

    public void verifyEmail(String token) throws MtgWalletGenericException {
        EmailVerificationToken verificationToken = oneTimeTokenService.consume(emailVerificationTokenRepository, token,
                GenericExceptionMessages.INVALID_EMAIL_VERIFICATION_TOKEN, GenericExceptionMessages.EXPIRED_EMAIL_VERIFICATION_TOKEN);
        WalletUser user = verificationToken.getUser();
        user.setIsEmailVerified(Boolean.TRUE); // managed: dirty checking saves it
        // A new user's starter categories and payees, in the same transaction (a no-op once created,
        // e.g. when a changed email is verified again)
        defaultDataService.createDefaultsFor(user);
    }
}
