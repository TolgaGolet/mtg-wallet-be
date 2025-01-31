package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.security.api.request.*;
import com.mtg.mtgwalletbe.security.api.response.AuthenticationResponse;
import com.mtg.mtgwalletbe.security.api.response.TotpSetupResponse;
import com.mtg.mtgwalletbe.security.service.AuthenticationService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!disabled-security")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationApi {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody @Validated RegisterRequest request) throws MtgWalletGenericException, MessagingException {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Validated AuthenticationRequest request) throws MtgWalletGenericException {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) throws MtgWalletGenericException {
        return ResponseEntity.ok(authenticationService.refreshToken(request, response));
    }

    @PostMapping("/totp/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(HttpServletRequest request)
            throws QrGenerationException, MtgWalletGenericException {
        return ResponseEntity.ok(authenticationService.setupTotp(request));
    }

    @PostMapping("/totp/verify-setup")
    public ResponseEntity<AuthenticationResponse> verifyAndEnableTotp(
            HttpServletRequest request,
            @RequestBody @Validated TotpSetupRequest totpRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(authenticationService.verifyAndEnableTotp(request, totpRequest.getCode()));
    }

    @PostMapping("/totp/verify")
    public ResponseEntity<AuthenticationResponse> verifyTotp(
            @RequestBody @Validated TotpVerificationRequest request) throws MtgWalletGenericException {
        return ResponseEntity.ok(authenticationService.verifyTotp(request));
    }

    @PostMapping("/totp/disable")
    public ResponseEntity<Void> disableTotp(HttpServletRequest request,
                                            @RequestBody @Validated TotpDisableRequest totpDisableRequest) throws MtgWalletGenericException {
        authenticationService.disableTotp(request, totpDisableRequest);
        return ResponseEntity.ok().build();
    }
}
