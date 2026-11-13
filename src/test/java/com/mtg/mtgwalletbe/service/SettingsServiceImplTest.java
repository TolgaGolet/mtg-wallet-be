package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.DeleteAccountRequest;
import com.mtg.mtgwalletbe.api.request.UpdateSettingsRequest;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.security.service.EmailVerificationService;
import com.mtg.mtgwalletbe.security.service.TotpService;
import com.mtg.mtgwalletbe.service.dto.SettingsDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceImplTest {

    @Mock private UserService userService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TotpService totpService;
    @InjectMocks private SettingsServiceImpl service;

    private static GenericExceptionMessages errorOf(Throwable e) {
        return ((MtgWalletGenericException) e).getError();
    }

    @Test
    void updateSettings_withNewEmail_createsVerificationToken() throws Exception {
        WalletUser user = WalletUser.builder().email("old@email.com").name("John").surname("Doe").totpEnabled(false).build();
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(userService.emailExists("new@email.com")).thenReturn(false);
        // the real UserService applies the change to the managed user; mimic it
        when(userService.updateProfile("John", "Doe", "new@email.com")).thenAnswer(inv -> {
            user.setEmail("new@email.com");
            return true;
        });

        SettingsDto result = service.updateSettings(UpdateSettingsRequest.builder().email("new@email.com").name("John").surname("Doe").build());

        verify(emailVerificationService).createVerificationToken(user);
        assertThat(result.getEmail()).isEqualTo("new@email.com");
    }

    @Test
    void updateSettings_withEmailOfAnotherAccount_isRejected() {
        WalletUser user = WalletUser.builder().email("old@email.com").build();
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(userService.emailExists("taken@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.updateSettings(UpdateSettingsRequest.builder().email("taken@email.com").name("John").surname("Doe").build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.EMAIL_ALREADY_EXISTS));
        verify(userService, never()).updateProfile(any(), any(), any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void deleteAccount_withTotpEnabled_validatesPasswordAndCode() throws Exception {
        WalletUser user = WalletUser.builder().password("hashedPassword").totpEnabled(true).totpSecret("secret").build();
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(totpService.verifyCode("123456", "secret")).thenReturn(true);

        service.deleteAccount(DeleteAccountRequest.builder().password("password").verificationCode("123456").build());

        verify(userService).anonymizeCurrentUser();
    }

    @Test
    void deleteAccount_withWrongPassword_isRejectedAndKeepsTheAccount() {
        WalletUser user = WalletUser.builder().password("hashedPassword").totpEnabled(false).build();
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteAccount(DeleteAccountRequest.builder().password("wrong").build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.WRONG_PASSWORD));
        verify(userService, never()).anonymizeCurrentUser();
    }

    @Test
    void deleteAccount_withTotpEnabledAndNoCode_isRejected() {
        WalletUser user = WalletUser.builder().password("hashedPassword").totpEnabled(true).totpSecret("secret").build();
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAccount(DeleteAccountRequest.builder().password("password").build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.INVALID_TOTP_CODE));
        verify(userService, never()).anonymizeCurrentUser();
    }

    @Test
    void getUserSettings_returnsCurrentUsersSettings() {
        WalletUserBasicDto user = new WalletUserBasicDto();
        user.setEmail("test@email.com");
        user.setName("John");
        user.setSurname("Doe");
        user.setTotpEnabled(true);
        when(userService.getCurrentLoggedInUser()).thenReturn(user);

        SettingsDto result = service.getUserSettings();

        assertThat(result.getEmail()).isEqualTo("test@email.com");
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getSurname()).isEqualTo("Doe");
        assertThat(result.getTotpEnabled()).isTrue();
    }
}
