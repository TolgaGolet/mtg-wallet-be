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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    @Override
    @Transactional(readOnly = true)
    public SettingsDto getUserSettings() {
        WalletUserBasicDto user = userService.getCurrentLoggedInUser();
        return settingsDto(user.getEmail(), user.getName(), user.getSurname(), user.getTotpEnabled());
    }

    @Override
    @Transactional
    public SettingsDto updateSettings(UpdateSettingsRequest request) throws MtgWalletGenericException {
        WalletUser user = userService.getCurrentUserEntity();
        if (!Objects.equals(request.getEmail(), user.getEmail()) && userService.emailExists(request.getEmail())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.EMAIL_ALREADY_EXISTS);
        }
        boolean emailChanged = userService.updateProfile(request.getName(), request.getSurname(), request.getEmail());
        if (emailChanged) {
            emailVerificationService.createVerificationToken(user);
        }
        return settingsDto(user.getEmail(), user.getName(), user.getSurname(), user.getTotpEnabled());
    }

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountRequest request) throws MtgWalletGenericException {
        WalletUser user = userService.getCurrentUserEntity();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.WRONG_PASSWORD);
        }
        if (Boolean.TRUE.equals(user.getTotpEnabled())
                && (request.getVerificationCode() == null || !totpService.verifyCode(request.getVerificationCode(), user.getTotpSecret()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.INVALID_TOTP_CODE);
        }
        userService.anonymizeCurrentUser();
    }

    private static SettingsDto settingsDto(String email, String name, String surname, Boolean totpEnabled) {
        return SettingsDto.builder().email(email).name(name).surname(surname).totpEnabled(totpEnabled).build();
    }
}
