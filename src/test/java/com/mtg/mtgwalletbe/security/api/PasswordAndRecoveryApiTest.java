package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.security.api.request.AccountRecoveryInitRequest;
import com.mtg.mtgwalletbe.security.service.AccountRecoveryService;
import com.mtg.mtgwalletbe.security.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import static com.mtg.mtgwalletbe.WebLayerTestSupport.*;
import static com.mtg.mtgwalletbe.security.SecurityParams.ERROR_CODE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PasswordResetApi and AccountRecoveryApi: public, unauthenticated endpoints, so input validation is their main job
class PasswordAndRecoveryApiTest {

    private final PasswordResetService passwordResetService = mock(PasswordResetService.class);
    private final AccountRecoveryService accountRecoveryService = mock(AccountRecoveryService.class);
    private MockMvc passwordReset;
    private MockMvc recovery;

    @BeforeEach
    void setUp() {
        passwordReset = mvcFor(new PasswordResetApi(passwordResetService));
        recovery = mvcFor(new AccountRecoveryApi(accountRecoveryService));
    }

    @Test
    void resetRequest_validEmail_is200() throws Exception {
        passwordReset.perform(post("/auth/password-reset/request").with(json("{'email':'" + VALID_EMAIL + "'}")))
                .andExpect(status().isOk());
        verify(passwordResetService).initiatePasswordReset(VALID_EMAIL);
    }

    @Test
    void resetRequest_invalidEmail_is400_withoutCallingTheService() throws Exception {
        passwordReset.perform(post("/auth/password-reset/request").with(json("{'email':'not an email'}")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_REQUEST"));
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void reset_passesPathTokenAndNewPassword() throws Exception {
        passwordReset.perform(post("/auth/password-reset/reset/tok-123").with(json("{'password':'" + VALID_PASSWORD + "'}")))
                .andExpect(status().isOk());
        verify(passwordResetService).resetPassword("tok-123", VALID_PASSWORD);
    }

    @Test
    void reset_weakPassword_is400_andTheTokenIsNotConsumed() throws Exception {
        passwordReset.perform(post("/auth/password-reset/reset/tok-123").with(json("{'password':'short'}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void reset_expiredToken_is400_withItsErrorCode() throws Exception {
        doThrow(new MtgWalletGenericException(GenericExceptionMessages.EXPIRED_PASSWORD_RESET_TOKEN))
                .when(passwordResetService).resetPassword("old", VALID_PASSWORD);

        passwordReset.perform(post("/auth/password-reset/reset/old").with(json("{'password':'" + VALID_PASSWORD + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "EXPIRED_PASSWORD_RESET_TOKEN"));
    }

    @Test
    void recoveryRequest_validCredentials_reachTheService() throws Exception {
        recovery.perform(post("/auth/account-recovery/request").with(json("{'username':'" + VALID_USERNAME + "','password':'" + VALID_PASSWORD + "'}")))
                .andExpect(status().isOk());
        ArgumentCaptor<AccountRecoveryInitRequest> request = ArgumentCaptor.forClass(AccountRecoveryInitRequest.class);
        verify(accountRecoveryService).initiateAccountRecovery(request.capture());
        assertThat(request.getValue().getUsername()).isEqualTo(VALID_USERNAME);
    }

    @Test
    void recoveryRequest_missingPassword_is400() throws Exception {
        recovery.perform(post("/auth/account-recovery/request").with(json("{'username':'" + VALID_USERNAME + "'}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountRecoveryService);
    }

    @Test
    void recover_passesThePathToken() throws Exception {
        recovery.perform(post("/auth/account-recovery/recover/tok-9")).andExpect(status().isOk());
        verify(accountRecoveryService).recoverAccount("tok-9");
    }
}
