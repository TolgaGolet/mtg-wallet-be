package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.DeleteAccountRequest;
import com.mtg.mtgwalletbe.api.request.UpdateSettingsRequest;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.security.service.AuthenticationService;
import com.mtg.mtgwalletbe.security.service.EmailVerificationService;
import com.mtg.mtgwalletbe.security.service.TotpService;
import com.mtg.mtgwalletbe.service.dto.SettingsDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private UserServiceMapper userServiceMapper;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TotpService totpService;

    private SettingsServiceImpl makeSettingsService() {
        return new SettingsServiceImpl(userService, emailVerificationService,
                userServiceMapper, authenticationService, passwordEncoder, totpService);
    }

    @Test
    void updateSettings_WithNewEmail_CreatesVerificationToken() throws Exception {
        // Arrange
        SettingsServiceImpl service = makeSettingsService();
        WalletUserDto currentUser = WalletUserDto.builder()
                .email("old@email.com")
                .name("John")
                .surname("Doe")
                .build();

        UpdateSettingsRequest request = UpdateSettingsRequest.builder()
                .email("new@email.com")
                .name("John")
                .surname("Doe")
                .build();

        WalletUserDto updatedUser = WalletUserDto.builder()
                .email("new@email.com")
                .name("John")
                .surname("Doe")
                .build();

        when(userService.getCurrentLoggedInUserFull()).thenReturn(currentUser);
        when(authenticationService.checkEmailExistence("new@email.com")).thenReturn(false);
        when(userServiceMapper.toWalletUserEntity((WalletUserDto) any())).thenReturn(new WalletUser());
        when(userService.updateUser(any())).thenReturn(updatedUser);

        // Act
        service.updateSettings(request);

        // Assert
        verify(emailVerificationService).createVerificationToken(any());
        verify(userService).updateUser(any());
    }

    @Test
    void deleteAccount_WithTotpEnabled_ValidatesPasswordAndCode() throws Exception {
        // Arrange
        SettingsServiceImpl service = makeSettingsService();
        WalletUserDto currentUser = WalletUserDto.builder()
                .password("hashedPassword")
                .totpEnabled(true)
                .totpSecret("secret")
                .build();
        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .password("password")
                .verificationCode("123456")
                .build();

        when(userService.getCurrentLoggedInUserFull()).thenReturn(currentUser);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(totpService.verifyCode("123456", "secret")).thenReturn(true);

        // Act
        service.deleteAccount(request);

        // Assert
        verify(passwordEncoder).matches("password", "hashedPassword");
        verify(totpService).verifyCode("123456", "secret");
        verify(userService).deleteUser(currentUser);
    }

    @Test
    void getUserSettings_ReturnsCorrectSettingsDto() {
        // Arrange
        SettingsServiceImpl service = makeSettingsService();
        WalletUserDto user = WalletUserDto.builder()
                .email("test@email.com")
                .name("John")
                .surname("Doe")
                .totpEnabled(true)
                .build();

        when(userService.getCurrentLoggedInUserFull()).thenReturn(user);

        // Act
        SettingsDto result = service.getUserSettings();

        // Assert
        assertEquals("test@email.com", result.getEmail());
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getSurname());
        assertTrue(result.getTotpEnabled());
    }
}