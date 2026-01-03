package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.security.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!disabled-security")
@RestController
@RequestMapping("/email-verification")
@RequiredArgsConstructor
public class EmailVerificationApi {
    private final EmailVerificationService emailVerificationService;
    private final WalletUserRepository userRepository;

    @PostMapping("/verify/{token}")
    public ResponseEntity<Void> verifyEmail(@PathVariable String token) throws MtgWalletGenericException {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend/{username}")
    public ResponseEntity<Void> resendVerificationEmail(@PathVariable String username) {
        WalletUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new UsernameNotFoundException(GenericExceptionMessages.ALREADY_VERIFIED_EMAIL.getMessage());
        }

        emailVerificationService.createVerificationToken(user);
        return ResponseEntity.ok().build();
    }
}
