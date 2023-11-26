package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.security.api.request.AuthenticationRequest;
import com.mtg.mtgwalletbe.security.api.request.RegisterRequest;
import com.mtg.mtgwalletbe.security.api.response.AuthenticationResponse;
import com.mtg.mtgwalletbe.security.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // TODO @Validated to all api requests?
    // TODO changing mapper strategy? Do we need to map dto to req or vice versa?
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) throws MtgWalletGenericException {
        try {
            return ResponseEntity.ok(authenticationService.register(request));
        } catch (MtgWalletGenericException e) {
            if (GenericExceptionMessages.USERNAME_ALREADY_EXISTS.getMessage().equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) throws MtgWalletGenericException {
        try {
            return ResponseEntity.ok(authenticationService.authenticate(request));
        } catch (MtgWalletGenericException e) {
            if (GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD.getMessage().equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                throw e;
            }
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) throws MtgWalletGenericException {
        try {
            return ResponseEntity.ok(authenticationService.refreshToken(request, response));
        } catch (MtgWalletGenericException e) {
            if (GenericExceptionMessages.JWT_EXPIRED.getMessage().equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                throw e;
            }
        }
    }
}
