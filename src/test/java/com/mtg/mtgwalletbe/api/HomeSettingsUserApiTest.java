package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.api.request.DeleteAccountRequest;
import com.mtg.mtgwalletbe.api.response.HomeScreenEnumResponse;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.service.HomeService;
import com.mtg.mtgwalletbe.service.SettingsService;
import com.mtg.mtgwalletbe.service.UserService;
import com.mtg.mtgwalletbe.service.dto.SettingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.mtg.mtgwalletbe.WebLayerTestSupport.*;
import static com.mtg.mtgwalletbe.security.SecurityParams.ERROR_CODE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// HomeApi, SettingsApi and UserApi (change-password) at the web layer; UserApi's admin rules are in UserApiAuthorizationTest
class HomeSettingsUserApiTest {

    private final HomeService homeService = mock(HomeService.class);
    private final SettingsService settingsService = mock(SettingsService.class);
    private final UserService userService = mock(UserService.class);
    private MockMvc home;
    private MockMvc settings;
    private MockMvc user;

    @BeforeEach
    void setUp() {
        home = mvcFor(new HomeApi(homeService));
        settings = mvcFor(new SettingsApi(settingsService));
        user = mvcFor(new UserApi(userService, mock(UserServiceMapper.class)));
    }

    // --- home ---

    @Test
    void homeEnums_returnCurrenciesInDeclaredOrder_andTheSavedPick() throws Exception {
        HomeScreenEnumResponse response = new HomeScreenEnumResponse();
        response.setCurrencies(List.of(Currency.values()));
        response.setSelectedCurrency(Currency.USD);
        when(homeService.getHomeScreenEnums()).thenReturn(response);

        home.perform(get("/home/enums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencies[0].value").value(Currency.values()[0].getValue()))
                .andExpect(jsonPath("$.selectedCurrency.value").value("USD"));
    }

    @Test
    void homeCurrency_is204_andPassesValueClientAndSequence() throws Exception {
        home.perform(post("/home/currency").with(json("{'currencyValue':'EUR','clientId':'tab-1','sequence':3}")))
                .andExpect(status().isNoContent());
        verify(homeService).updateHomeCurrency("EUR", "tab-1", 3L);
    }

    @Test
    void homeCurrency_negativeSequence_is400() throws Exception {
        home.perform(post("/home/currency").with(json("{'currencyValue':'EUR','clientId':'tab-1','sequence':-1}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(homeService);
    }

    @Test
    void homeCurrency_missingCurrency_is400() throws Exception {
        home.perform(post("/home/currency").with(json("{}")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_REQUEST"));
        verifyNoInteractions(homeService);
    }

    @Test
    void netValue_missingCurrencyParam_is400() throws Exception {
        home.perform(get("/home/net-value").param("intervalValue", "M"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_REQUEST"));
        verifyNoInteractions(homeService);
    }

    @Test
    void netValue_passesBothParams() throws Exception {
        home.perform(get("/home/net-value").param("currencyValue", "TRY").param("intervalValue", "M"))
                .andExpect(status().isOk());
        verify(homeService).getNetValue("TRY", "M");
    }

    // --- settings ---

    // The settings screen shows these; the password hash / TOTP secret are not part of the DTO at all
    @Test
    void getSettings_returnsProfileAndTotpFlag() throws Exception {
        when(settingsService.getUserSettings()).thenReturn(SettingsDto.builder().email(VALID_EMAIL).name("Alice").totpEnabled(true).build());

        settings.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.totpEnabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.totpSecret").doesNotExist());
    }

    @Test
    void settingsUpdate_returnsTheSavedSettings() throws Exception {
        SettingsDto saved = SettingsDto.builder().email(VALID_EMAIL).name("Alice").build();
        when(settingsService.updateSettings(any())).thenReturn(saved);

        settings.perform(post("/settings/update").with(json("{'email':'" + VALID_EMAIL + "','name':'Alice','surname':'A'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(VALID_EMAIL));
    }

    @Test
    void settingsUpdate_tooShortName_is400() throws Exception {
        settings.perform(post("/settings/update").with(json("{'email':'" + VALID_EMAIL + "','name':'Al','surname':'A'}")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("name")));
        verifyNoInteractions(settingsService);
    }

    @Test
    void deleteAccount_is204_andPassesPasswordAndCode() throws Exception {
        settings.perform(delete("/settings/account").with(json("{'password':'" + VALID_PASSWORD + "','verificationCode':'123456'}")))
                .andExpect(status().isNoContent());
        ArgumentCaptor<DeleteAccountRequest> request = ArgumentCaptor.forClass(DeleteAccountRequest.class);
        verify(settingsService).deleteAccount(request.capture());
        assertThat(request.getValue().getPassword()).isEqualTo(VALID_PASSWORD);
        assertThat(request.getValue().getVerificationCode()).isEqualTo("123456");
    }

    @Test
    void deleteAccount_wrongPassword_is400_never401() throws Exception {
        doThrow(new MtgWalletGenericException(GenericExceptionMessages.WRONG_PASSWORD)).when(settingsService).deleteAccount(any());

        settings.perform(delete("/settings/account").with(json("{'password':'" + VALID_PASSWORD + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "WRONG_PASSWORD"));
    }

    @Test
    void deleteAccount_malformedTotpCode_is400() throws Exception {
        settings.perform(delete("/settings/account").with(json("{'password':'" + VALID_PASSWORD + "','verificationCode':'12'}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(settingsService);
    }

    // --- user ---

    private static String changePasswordBody(String newPassword) {
        return "{'currentPassword':'" + VALID_PASSWORD + "','newPassword':'" + newPassword + "','confirmPassword':'" + newPassword + "'}";
    }

    @Test
    void changePassword_validBody_reachesTheService() throws Exception {
        user.perform(post("/user/change-password").with(json(changePasswordBody("N3wPass!x"))))
                .andExpect(status().isOk());
        ArgumentCaptor<ChangePasswordRequest> request = ArgumentCaptor.forClass(ChangePasswordRequest.class);
        verify(userService).changePassword(request.capture());
        assertThat(request.getValue().getNewPassword()).isEqualTo("N3wPass!x");
    }

    @Test
    void changePassword_wrongCurrentPassword_is400_never401() throws Exception {
        doThrow(new MtgWalletGenericException(GenericExceptionMessages.WRONG_PASSWORD)).when(userService).changePassword(any());

        user.perform(post("/user/change-password").with(json(changePasswordBody("N3wPass!x"))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "WRONG_PASSWORD"));
    }

    @Test
    void changePassword_weakNewPassword_is400() throws Exception {
        user.perform(post("/user/change-password").with(json(changePasswordBody("weak"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }
}
