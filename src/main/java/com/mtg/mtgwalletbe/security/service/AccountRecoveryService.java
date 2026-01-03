package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.AccountRecoveryToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.AccountRecoveryTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.security.api.request.AccountRecoveryInitRequest;
import com.mtg.mtgwalletbe.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {
    private final AccountRecoveryTokenRepository accountRecoveryTokenRepository;
    private final WalletUserRepository userRepository;
    private final MailService mailService;
    private final AuthenticationService authenticationService;

    @Value("${mtgWallet.frontend.url}")
    private String frontendUrl;

    private static final int TOKEN_EXPIRATION_MINUTES = 10;

    @Transactional
    public void initiateAccountRecovery(AccountRecoveryInitRequest request) throws MtgWalletGenericException {
        WalletUser user = authenticationService.authenticateUser(request.getUsername(), request.getPassword());
        accountRecoveryTokenRepository.findByUser(user).ifPresent(accountRecoveryTokenRepository::delete);
        accountRecoveryTokenRepository.flush();
        String token = UUID.randomUUID().toString();
        AccountRecoveryToken recoveryToken = AccountRecoveryToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES))
                .build();

        accountRecoveryTokenRepository.save(recoveryToken);
        sendAccountRecoveryEmail(user.getEmail(), token);
    }

    private void sendAccountRecoveryEmail(String email, String token) {
        String recoveryUrl = frontendUrl + "/recover-account/" + token;
        String emailContent = String.format("""
                <p>You have requested to recover your account. <b>This action will disable your two factor authentication.</b> You should enable this feature once you login for your security.</p>
                <p>Please click the link below to proceed:</p>
                <p><a href="%s">Recover Account</a></p>
                <p>This link will expire in %d minutes.</p>
                <p><b>If you did not request this account recovery, change your password immediately through the "Forgot Password?" page.</b></p>
                """, recoveryUrl, TOKEN_EXPIRATION_MINUTES);

        mailService.send(email, "Account Recovery Request", emailContent);
    }

    @Transactional
    public void recoverAccount(String token) throws MtgWalletGenericException {
        AccountRecoveryToken recoveryToken = validateAndGetToken(token);

        WalletUser user = recoveryToken.getUser();
        user.setTotpEnabled(Boolean.FALSE);
        userRepository.save(user);

        accountRecoveryTokenRepository.delete(recoveryToken);
    }

    private AccountRecoveryToken validateAndGetToken(String token) throws MtgWalletGenericException {
        AccountRecoveryToken recoveryToken = accountRecoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.INVALID_ACCOUNT_RECOVERY_TOKEN.getMessage()));

        if (recoveryToken.isExpired()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.EXPIRED_ACCOUNT_RECOVERY_TOKEN.getMessage());
        }

        return recoveryToken;
    }
}
