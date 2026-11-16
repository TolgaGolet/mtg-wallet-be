package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.security.api.request.AuthenticationRequest;
import com.mtg.mtgwalletbe.security.api.request.RegisterRequest;
import com.mtg.mtgwalletbe.security.api.request.TotpDisableRequest;
import com.mtg.mtgwalletbe.security.api.response.AuthenticationResponse;
import com.mtg.mtgwalletbe.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import static com.mtg.mtgwalletbe.WebLayerTestSupport.*;
import static com.mtg.mtgwalletbe.security.SecurityParams.ERROR_CODE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationApiTest {

    private final AuthenticationService service = mock(AuthenticationService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mvcFor(new AuthenticationApi(service));
    }

    private static String loginBody(String username, String password) {
        return "{'username':'" + username + "','password':'" + password + "'}";
    }

    @Test
    void register_validBody_returnsTheServiceResponse() throws Exception {
        when(service.register(any())).thenReturn(AuthenticationResponse.builder().accessToken("t").emailVerificationRequired(true).build());

        mvc.perform(post("/auth/register").with(json(
                        "{'username':'" + VALID_USERNAME + "','email':'" + VALID_EMAIL + "','name':'Alice','password':'" + VALID_PASSWORD + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("t"))
                .andExpect(jsonPath("$.emailVerificationRequired").value(true));

        ArgumentCaptor<RegisterRequest> request = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(service).register(request.capture());
        assertThat(request.getValue().getEmail()).isEqualTo(VALID_EMAIL);
    }

    @Test
    void register_weakPassword_is400_withoutCallingTheService() throws Exception {
        mvc.perform(post("/auth/register").with(json(
                        "{'username':'" + VALID_USERNAME + "','email':'" + VALID_EMAIL + "','name':'Alice','password':'password'}")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_REQUEST"))
                .andExpect(content().string(containsString("password")));
        verifyNoInteractions(service);
    }

    @Test
    void register_usernameWithSymbols_is400() throws Exception {
        mvc.perform(post("/auth/register").with(json(
                        "{'username':'al ice!','email':'alice@example.com','name':'Alice','password':'" + VALID_PASSWORD + "'}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void authenticate_validBody_returnsTokens() throws Exception {
        when(service.authenticate(any())).thenReturn(AuthenticationResponse.builder().accessToken("a").refreshToken("r").build());

        mvc.perform(post("/auth/authenticate").with(json(loginBody(VALID_USERNAME, VALID_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a"))
                .andExpect(jsonPath("$.refreshToken").value("r"));
        ArgumentCaptor<AuthenticationRequest> request = ArgumentCaptor.forClass(AuthenticationRequest.class);
        verify(service).authenticate(request.capture());
        assertThat(request.getValue().getUsername()).isEqualTo(VALID_USERNAME);
    }

    // The frontend refreshes tokens on any 401/403, so a wrong password must be a 400
    @Test
    void authenticate_badCredentials_is400_withErrorCode_never401() throws Exception {
        when(service.authenticate(any())).thenThrow(new MtgWalletGenericException(GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD));

        mvc.perform(post("/auth/authenticate").with(json(loginBody(VALID_USERNAME, VALID_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "BAD_USERNAME_OR_PASSWORD"));
    }

    @Test
    void authenticate_unverifiedEmail_carriesTheCodeTheFrontendBranchesOn() throws Exception {
        when(service.authenticate(any())).thenThrow(new MtgWalletGenericException(GenericExceptionMessages.NOT_VERIFIED_EMAIL));

        mvc.perform(post("/auth/authenticate").with(json(loginBody(VALID_USERNAME, VALID_PASSWORD))))
                .andExpect(header().string(ERROR_CODE_HEADER, "NOT_VERIFIED_EMAIL"));
    }

    @Test
    void authenticate_missingPassword_is400() throws Exception {
        mvc.perform(post("/auth/authenticate").with(json("{'username':'" + VALID_USERNAME + "'}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void refreshToken_passesTheRequestToTheService() throws Exception {
        when(service.refreshToken(any())).thenReturn(AuthenticationResponse.builder().accessToken("new").build());

        mvc.perform(post("/auth/refresh-token").header("Authorization", "Bearer x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new"));
    }

    @Test
    void refreshToken_expired_is400_withJwtExpired() throws Exception {
        when(service.refreshToken(any())).thenThrow(new MtgWalletGenericException(GenericExceptionMessages.JWT_EXPIRED));

        mvc.perform(post("/auth/refresh-token").header("Authorization", "Bearer x"))
                .andExpect(header().string(ERROR_CODE_HEADER, "JWT_EXPIRED"));
    }

    @Test
    void totpSetup_returnsSecretAndQrCode() throws Exception {
        when(service.setupTotp(any())).thenReturn(com.mtg.mtgwalletbe.security.api.response.TotpSetupResponse.builder()
                .secret("SECRET").qrCodeImage("qr").build());

        mvc.perform(post("/auth/totp/setup").header("Authorization", "Bearer x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("SECRET"))
                .andExpect(jsonPath("$.qrCodeImage").value("qr"));
    }

    @Test
    void totpVerifySetup_passesTheCode() throws Exception {
        when(service.verifyAndEnableTotp(any(), eq("123456"))).thenReturn(AuthenticationResponse.builder().accessToken("a").build());

        mvc.perform(post("/auth/totp/verify-setup").with(json("{'code':'123456'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a"));
        verify(service).verifyAndEnableTotp(any(), eq("123456"));
    }

    @Test
    void totpVerifySetup_nonNumericCode_is400() throws Exception {
        mvc.perform(post("/auth/totp/verify-setup").with(json("{'code':'12a456'}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void totpVerify_wrongCode_is400_withInvalidTotpCode() throws Exception {
        when(service.verifyTotp(any())).thenThrow(new MtgWalletGenericException(GenericExceptionMessages.INVALID_TOTP_CODE));

        mvc.perform(post("/auth/totp/verify").with(json(
                        "{'username':'alice','password':'" + VALID_PASSWORD + "','verificationCode':'000000'}")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_TOTP_CODE"));
    }

    @Test
    void totpDisable_returnsEmpty200() throws Exception {
        mvc.perform(post("/auth/totp/disable").with(json("{'code':'123456'}")))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        ArgumentCaptor<TotpDisableRequest> request = ArgumentCaptor.forClass(TotpDisableRequest.class);
        verify(service).disableTotp(any(), request.capture());
        assertThat(request.getValue().getCode()).isEqualTo("123456");
    }
}
