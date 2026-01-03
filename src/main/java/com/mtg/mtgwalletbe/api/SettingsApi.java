package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.DeleteAccountRequest;
import com.mtg.mtgwalletbe.api.request.UpdateSettingsRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.SettingsService;
import com.mtg.mtgwalletbe.service.dto.SettingsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsApi {
    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<SettingsDto> getUserSettings() {
        return ResponseEntity.ok(settingsService.getUserSettings());
    }

    @PostMapping("/update")
    public ResponseEntity<SettingsDto> updateSettings(@RequestBody @Validated UpdateSettingsRequest request)
            throws MtgWalletGenericException {
        return ResponseEntity.ok(settingsService.updateSettings(request));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestBody @Validated DeleteAccountRequest request) throws MtgWalletGenericException {
        settingsService.deleteAccount(request);
        return ResponseEntity.noContent().build();
    }
}
