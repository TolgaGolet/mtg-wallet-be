package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.DeleteAccountRequest;
import com.mtg.mtgwalletbe.api.request.UpdateSettingsRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.SettingsDto;

public interface SettingsService {
    SettingsDto getUserSettings();

    SettingsDto updateSettings(UpdateSettingsRequest request) throws MtgWalletGenericException;

    void deleteAccount(DeleteAccountRequest request) throws MtgWalletGenericException;
}
