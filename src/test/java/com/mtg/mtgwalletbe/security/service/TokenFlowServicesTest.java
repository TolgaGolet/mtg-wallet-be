package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.config.FrontendProperties;
import com.mtg.mtgwalletbe.entity.AccountRecoveryToken;
import com.mtg.mtgwalletbe.entity.EmailVerificationToken;
import com.mtg.mtgwalletbe.entity.PasswordResetToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.AccountRecoveryTokenRepository;
import com.mtg.mtgwalletbe.repository.EmailVerificationTokenRepository;
import com.mtg.mtgwalletbe.repository.PasswordResetTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.security.api.request.AccountRecoveryInitRequest;
import com.mtg.mtgwalletbe.service.DefaultDataService;
import com.mtg.mtgwalletbe.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// F7: the three emailed-token flows had no tests (expiry and single use come from OneTimeTokenService)
class TokenFlowServicesTest {

    private final OneTimeTokenService tokens = mock(OneTimeTokenService.class);
    private final UserService userService = mock(UserService.class);
    private WalletUser user;

    @BeforeEach
    void setUp() {
        user = WalletUser.builder().id(1L).email("a@example.com").password("old").isEmailVerified(false).totpEnabled(true).build();
        when(tokens.issue(any(), any(), any(), any())).thenReturn("tok");
    }

    private static final FrontendProperties FRONTEND = new FrontendProperties("https://app.example");

    private String sentHtml(String to, String subject) {
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(tokens).sendAfterCommit(eq(to), eq(subject), html.capture());
        return html.getValue();
    }

    // ---- password reset ----

    private final PasswordResetTokenRepository resetRepository = mock(PasswordResetTokenRepository.class);
    private final WalletUserRepository userRepository = mock(WalletUserRepository.class);

    private PasswordResetService passwordResetService() {
        return new PasswordResetService(resetRepository, userRepository, tokens, userService, FRONTEND);
    }

    @Test
    void initiatePasswordReset_issues15MinuteToken_andMailsTheLink() {
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));

        passwordResetService().initiatePasswordReset("a@example.com");

        verify(tokens).issue(eq(resetRepository), any(), eq(user), eq(Duration.ofMinutes(15)));
        assertThat(sentHtml("a@example.com", "Password Reset Request")).contains("https://app.example/reset-password/tok");
    }

    @Test
    void initiatePasswordReset_unknownEmail_isSilent() {
        when(userRepository.findByEmail("x@example.com")).thenReturn(Optional.empty());

        passwordResetService().initiatePasswordReset("x@example.com");

        verifyNoInteractions(tokens);
    }

    // Regression for F9: a reset left every existing session valid
    @Test
    void resetPassword_replacesPassword_whichRevokesAllSessions() throws Exception {
        PasswordResetToken stored = new PasswordResetToken();
        stored.setUser(user);
        when(tokens.consume(eq(resetRepository), eq("tok"), any(), any())).thenReturn(stored);

        passwordResetService().resetPassword("tok", "New1!pass");

        verify(userService).replacePassword(user, "New1!pass");
    }

    @Test
    void resetPassword_invalidToken_changesNothing() throws Exception {
        when(tokens.consume(eq(resetRepository), eq("bad"), any(), any()))
                .thenThrow(new MtgWalletGenericException(GenericExceptionMessages.INVALID_PASSWORD_RESET_TOKEN));

        assertThatThrownBy(() -> passwordResetService().resetPassword("bad", "New1!pass"))
                .isInstanceOf(MtgWalletGenericException.class);
        assertThat(user.getPassword()).isEqualTo("old");
        verifyNoInteractions(userService);
    }

    // ---- email verification ----

    private final EmailVerificationTokenRepository verificationRepository = mock(EmailVerificationTokenRepository.class);
    private final DefaultDataService defaultDataService = mock(DefaultDataService.class);

    private EmailVerificationService emailVerificationService() {
        return new EmailVerificationService(verificationRepository, tokens, defaultDataService, FRONTEND);
    }

    @Test
    void createVerificationToken_issues24HourToken_andMailsTheLink() {
        emailVerificationService().createVerificationToken(user);

        verify(tokens).issue(eq(verificationRepository), any(), eq(user), eq(Duration.ofHours(24)));
        assertThat(sentHtml("a@example.com", "Verify Your Email Address")).contains("https://app.example/verify-email/tok");
    }

    @Test
    void verifyEmail_marksTheTokenOwnerVerified() throws Exception {
        EmailVerificationToken stored = new EmailVerificationToken();
        stored.setUser(user);
        when(tokens.consume(eq(verificationRepository), eq("tok"),
                eq(GenericExceptionMessages.INVALID_EMAIL_VERIFICATION_TOKEN), eq(GenericExceptionMessages.EXPIRED_EMAIL_VERIFICATION_TOKEN)))
                .thenReturn(stored);

        emailVerificationService().verifyEmail("tok");

        assertThat(user.getIsEmailVerified()).isTrue();
    }

    // The frontend used to call POST /user/create-defaults after every login (F14)
    @Test
    void verifyEmail_createsTheNewUsersDefaults() throws Exception {
        EmailVerificationToken stored = new EmailVerificationToken();
        stored.setUser(user);
        when(tokens.consume(eq(verificationRepository), eq("tok"),
                eq(GenericExceptionMessages.INVALID_EMAIL_VERIFICATION_TOKEN), eq(GenericExceptionMessages.EXPIRED_EMAIL_VERIFICATION_TOKEN)))
                .thenReturn(stored);

        emailVerificationService().verifyEmail("tok");

        verify(defaultDataService).createDefaultsFor(user);
    }

    @Test
    void verifyEmail_withAnInvalidTokenCreatesNoDefaults() throws Exception {
        when(tokens.consume(eq(verificationRepository), eq("bad"), any(), any()))
                .thenThrow(new MtgWalletGenericException(GenericExceptionMessages.INVALID_EMAIL_VERIFICATION_TOKEN));

        assertThatThrownBy(() -> emailVerificationService().verifyEmail("bad"))
                .isInstanceOf(MtgWalletGenericException.class);
        verifyNoInteractions(defaultDataService);
    }

    // ---- account recovery ----

    private final AccountRecoveryTokenRepository recoveryRepository = mock(AccountRecoveryTokenRepository.class);
    private final AuthenticationService authenticationService = mock(AuthenticationService.class);

    private AccountRecoveryService accountRecoveryService() {
        return new AccountRecoveryService(recoveryRepository, authenticationService, tokens, FRONTEND);
    }

    @Test
    void initiateAccountRecovery_requiresCredentials() throws Exception {
        when(authenticationService.authenticateUser("alice", "wrong"))
                .thenThrow(new MtgWalletGenericException(GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD));
        AccountRecoveryInitRequest request = new AccountRecoveryInitRequest();
        request.setUsername("alice");
        request.setPassword("wrong");

        assertThatThrownBy(() -> accountRecoveryService().initiateAccountRecovery(request))
                .isInstanceOf(MtgWalletGenericException.class);
        verifyNoInteractions(tokens);
    }

    @Test
    void initiateAccountRecovery_issues10MinuteToken_andMailsTheLink() throws Exception {
        when(authenticationService.authenticateUser("alice", "pw")).thenReturn(user);
        AccountRecoveryInitRequest request = new AccountRecoveryInitRequest();
        request.setUsername("alice");
        request.setPassword("pw");

        accountRecoveryService().initiateAccountRecovery(request);

        verify(tokens).issue(eq(recoveryRepository), any(), eq(user), eq(Duration.ofMinutes(10)));
        assertThat(sentHtml("a@example.com", "Account Recovery Request")).contains("https://app.example/recover-account/tok");
    }

    @Test
    void recoverAccount_disablesTotp() throws Exception {
        AccountRecoveryToken stored = new AccountRecoveryToken();
        stored.setUser(user);
        when(tokens.consume(eq(recoveryRepository), eq("tok"), any(), any())).thenReturn(stored);

        accountRecoveryService().recoverAccount("tok");

        assertThat(user.getTotpEnabled()).isFalse();
    }
}
