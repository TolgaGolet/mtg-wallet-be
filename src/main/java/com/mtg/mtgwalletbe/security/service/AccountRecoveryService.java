package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.config.FrontendProperties;
import com.mtg.mtgwalletbe.entity.AccountRecoveryToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.AccountRecoveryTokenRepository;
import com.mtg.mtgwalletbe.security.api.request.AccountRecoveryInitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Profile("!disabled-security") // needs AuthenticationService, which only exists with security enabled
@Service
@RequiredArgsConstructor
@Transactional
public class AccountRecoveryService {
    private final AccountRecoveryTokenRepository accountRecoveryTokenRepository;
    private final AuthenticationService authenticationService;
    private final OneTimeTokenService oneTimeTokenService;
    private final FrontendProperties frontendProperties;

    private static final int TOKEN_EXPIRATION_MINUTES = 10;

    public void initiateAccountRecovery(AccountRecoveryInitRequest request) throws MtgWalletGenericException {
        WalletUser user = authenticationService.authenticateUser(request.getUsername(), request.getPassword());
        String token = oneTimeTokenService.issue(accountRecoveryTokenRepository, AccountRecoveryToken::new,
                user, Duration.ofMinutes(TOKEN_EXPIRATION_MINUTES));
        String recoveryUrl = frontendProperties.url() + "/recover-account/" + token;
        String emailContent = String.format("""
                <p>You have requested to recover your account. <b>This action will disable your two factor authentication.</b> You should enable this feature once you login for your security.</p>
                <p>Please click the link below to proceed:</p>
                <p><a href="%s">Recover Account</a></p>
                <p>This link will expire in %d minutes.</p>
                <p><b>If you did not request this account recovery, change your password immediately through the "Forgot Password?" page.</b></p>
                """, recoveryUrl, TOKEN_EXPIRATION_MINUTES);
        oneTimeTokenService.sendAfterCommit(user.getEmail(), "Account Recovery Request", emailContent);
    }

    public void recoverAccount(String token) throws MtgWalletGenericException {
        AccountRecoveryToken recoveryToken = oneTimeTokenService.consume(accountRecoveryTokenRepository, token,
                GenericExceptionMessages.INVALID_ACCOUNT_RECOVERY_TOKEN, GenericExceptionMessages.EXPIRED_ACCOUNT_RECOVERY_TOKEN);
        recoveryToken.getUser().setTotpEnabled(Boolean.FALSE); // managed: dirty checking saves it
    }
}
