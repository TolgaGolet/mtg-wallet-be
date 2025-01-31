package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.DeleteAccountRequest;
import com.mtg.mtgwalletbe.api.request.UpdateSettingsRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.SettingsDto;
import jakarta.mail.MessagingException;

public interface SettingsService {
    SettingsDto getUserSettings();

    SettingsDto updateSettings(UpdateSettingsRequest request) throws MtgWalletGenericException, MessagingException;

    void deleteAccount(DeleteAccountRequest request) throws MtgWalletGenericException;
}
