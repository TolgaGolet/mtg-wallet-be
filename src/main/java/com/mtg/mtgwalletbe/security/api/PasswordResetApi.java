package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.security.api.request.PasswordResetRequest;
import com.mtg.mtgwalletbe.security.api.request.ResetPasswordInitRequest;
import com.mtg.mtgwalletbe.security.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("!disabled-security")
@RestController
@RequestMapping("/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetApi {
    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid ResetPasswordInitRequest request) {
        passwordResetService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset/{token}")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String token,
            @RequestBody @Valid PasswordResetRequest request) throws MtgWalletGenericException {
        passwordResetService.resetPassword(token, request.getPassword());
        return ResponseEntity.ok().build();
    }
}
