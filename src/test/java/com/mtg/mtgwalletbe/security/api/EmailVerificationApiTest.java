package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.security.service.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationApiTest {
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private WalletUserRepository userRepository;
    @InjectMocks
    private EmailVerificationApi emailVerificationApi;

    // Regression: this used to throw UsernameNotFoundException, which the handler answers as 404 USER_NOT_FOUND
    @Test
    void resend_forAlreadyVerifiedEmail_throwsAlreadyVerified() {
        WalletUser user = new WalletUser();
        user.setIsEmailVerified(true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> emailVerificationApi.resendVerificationEmail("alice"))
                .isInstanceOfSatisfying(MtgWalletGenericException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getError())
                                .isEqualTo(GenericExceptionMessages.ALREADY_VERIFIED_EMAIL));
        verify(emailVerificationService, never()).createVerificationToken(any());
    }

    @Test
    void resend_forUnknownUser_throwsUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationApi.resendVerificationEmail("ghost"))
                .isInstanceOfSatisfying(MtgWalletGenericException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getError())
                                .isEqualTo(GenericExceptionMessages.USER_NOT_FOUND));
    }
}
