package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.repository.RoleRepository;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// CQ-08 / CQ-09: current user comes from the principal; user changes are targeted, not DTO copies
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock private UserServiceMapper mapper;
    @Mock private WalletUserRepository walletUserRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserTokenRepository userTokenRepository;
    @InjectMocks private UserServiceImpl service;

    private WalletUser principal;
    private WalletUser managed;

    @BeforeEach
    void setUp() {
        principal = WalletUser.builder().id(7L).username("alice").build();
        managed = WalletUser.builder().id(7L).username("alice").password("hash").name("Alice").surname("A").email("a@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(walletUserRepository.findById(7L)).thenReturn(Optional.of(managed));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUser_comesFromPrincipal_withoutQuery() {
        WalletUserBasicDto dto = new WalletUserBasicDto();
        dto.setId(7L);
        when(mapper.toWalletUserBasicDto(principal)).thenReturn(dto);

        assertThat(service.getCurrentUserId()).isEqualTo(7L);
        assertThat(service.getCurrentLoggedInUser()).isSameAs(dto);
        verifyNoInteractions(walletUserRepository);
    }

    @Test
    void validateUserId_ofOtherUser_isRejected_withoutQuery() throws Exception {
        service.validateUserIdIfItsTheCurrentUser(7L);

        assertThatThrownBy(() -> service.validateUserIdIfItsTheCurrentUser(8L))
                .isInstanceOf(MtgWalletGenericException.class)
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM);
        verifyNoInteractions(walletUserRepository);
    }

    @Test
    void changePassword_wrongCurrentPassword_isRejected() {
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        ChangePasswordRequest request = ChangePasswordRequest.builder().currentPassword("wrong").newPassword("New1!pass").confirmPassword("New1!pass").build();

        assertThatThrownBy(() -> service.changePassword(request))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.WRONG_PASSWORD);
        assertThat(managed.getPassword()).isEqualTo("hash");
    }

    @Test
    void changePassword_ok_updatesHash() throws Exception {
        when(passwordEncoder.matches("current", "hash")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
        ChangePasswordRequest request = ChangePasswordRequest.builder().currentPassword("current").newPassword("New1!pass").confirmPassword("New1!pass").build();

        service.changePassword(request);

        assertThat(managed.getPassword()).isEqualTo("new-hash");
    }

    @Test
    void updateProfile_newEmail_isMarkedUnverified() {
        managed.setIsEmailVerified(Boolean.TRUE);

        boolean changed = service.updateProfile("Alicia", "B", "new@example.com");

        assertThat(changed).isTrue();
        assertThat(managed.getEmail()).isEqualTo("new@example.com");
        assertThat(managed.getIsEmailVerified()).isFalse();
        assertThat(managed.getName()).isEqualTo("Alicia");
    }

    @Test
    void updateProfile_sameEmail_keepsVerification() {
        managed.setIsEmailVerified(Boolean.TRUE);

        assertThat(service.updateProfile("Alice", "A", "a@example.com")).isFalse();
        assertThat(managed.getIsEmailVerified()).isTrue();
    }

    @Test
    void anonymize_removesCredentialsAndRevokesSessions() {
        managed.setTotpSecret("SECRET");
        managed.setTotpEnabled(Boolean.TRUE);
        UserToken session = UserToken.builder().token("t").revoked(false).expired(false).build();
        when(userTokenRepository.findAllNotFullyRevokedTokensByUserId(7L)).thenReturn(List.of(session));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        service.anonymizeCurrentUser();

        assertThat(managed.getUsername()).startsWith("DLT#");
        assertThat(managed.getEmail()).startsWith("DLT#");
        assertThat(managed.getPassword()).isEqualTo("random-hash");
        assertThat(managed.getTotpSecret()).isNull();
        assertThat(managed.getTotpEnabled()).isFalse();
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.isExpired()).isTrue();
    }
}
