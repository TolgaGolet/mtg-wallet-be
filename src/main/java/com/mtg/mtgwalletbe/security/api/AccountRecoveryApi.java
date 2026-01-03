package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.security.api.request.AccountRecoveryInitRequest;
import com.mtg.mtgwalletbe.security.service.AccountRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("!disabled-security")
@RestController
@RequestMapping("/auth/account-recovery")
@RequiredArgsConstructor
public class AccountRecoveryApi {
    private final AccountRecoveryService accountRecoveryService;

    @PostMapping("/request")
    public ResponseEntity<Void> requestAccountRecovery(@RequestBody @Valid AccountRecoveryInitRequest request)
            throws MtgWalletGenericException {
        accountRecoveryService.initiateAccountRecovery(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recover/{token}")
    public ResponseEntity<Void> recoverAccount(
            @PathVariable String token) throws MtgWalletGenericException {
        accountRecoveryService.recoverAccount(token);
        return ResponseEntity.ok().build();
    }
}
