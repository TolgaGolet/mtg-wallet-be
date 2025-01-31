package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.DeleteAccountRequest;
import com.mtg.mtgwalletbe.api.request.UpdateSettingsRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.security.service.AuthenticationService;
import com.mtg.mtgwalletbe.security.service.EmailVerificationService;
import com.mtg.mtgwalletbe.security.service.TotpService;
import com.mtg.mtgwalletbe.service.dto.SettingsDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final UserServiceMapper userServiceMapper;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    @Override
    @Transactional(readOnly = true)
    public SettingsDto getUserSettings() {
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUserFull();
        return mapToSettingsDto(walletUserDto);
    }

    @Override
    @Transactional
    public SettingsDto updateSettings(UpdateSettingsRequest request) throws MtgWalletGenericException, MessagingException {
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUserFull();

        if (!request.getEmail().equals(walletUserDto.getEmail())) {
            if (Boolean.TRUE.equals(authenticationService.checkEmailExistence(request.getEmail()))) {
                throw new MtgWalletGenericException(GenericExceptionMessages.EMAIL_ALREADY_EXISTS.getMessage());
            }
            walletUserDto.setEmail(request.getEmail());
            walletUserDto.setIsEmailVerified(false);
            emailVerificationService.createVerificationToken(userServiceMapper.toWalletUserEntity(walletUserDto));
        }

        if (!request.getName().equals(walletUserDto.getName())) {
            walletUserDto.setName(request.getName());
        }

        if (!request.getSurname().equals(walletUserDto.getSurname())) {
            walletUserDto.setSurname(request.getSurname());
        }

        return mapToSettingsDto(userService.updateUser(walletUserDto));
    }

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountRequest request) throws MtgWalletGenericException {
        WalletUserDto currentUser = userService.getCurrentLoggedInUserFull();

        if (currentUser == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.USER_NOT_FOUND.getMessage());
        }

        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
            throw new IllegalStateException(GenericExceptionMessages.WRONG_PASSWORD.getMessage());
        }

        if (Boolean.TRUE.equals(currentUser.getTotpEnabled())) {
            if (request.getVerificationCode() == null) {
                throw new MtgWalletGenericException(GenericExceptionMessages.INVALID_TOTP_CODE.getMessage());
            }
            if (!totpService.verifyCode(request.getVerificationCode(), currentUser.getTotpSecret())) {
                throw new MtgWalletGenericException(GenericExceptionMessages.INVALID_TOTP_CODE.getMessage());
            }
        }

        userService.deleteUser(currentUser);
    }

    private SettingsDto mapToSettingsDto(WalletUserDto user) {
        return SettingsDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .surname(user.getSurname())
                .totpEnabled(user.getTotpEnabled())
                .build();
    }
}
