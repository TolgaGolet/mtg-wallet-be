package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Currency;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        when(walletUserRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(managed));
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
    void homeCurrency_isReadFromPrincipal_andSavedOnManagedEntity() {
        principal.setHomeCurrency(Currency.USD);

        assertThat(service.getHomeCurrency()).isEqualTo(Currency.USD);
        verifyNoInteractions(walletUserRepository);

        service.setHomeCurrency(Currency.TRY, "tab-1", 3L);

        assertThat(managed.getHomeCurrency()).isEqualTo(Currency.TRY);
        assertThat(managed.getHomeCurrencyClientId()).isEqualTo("tab-1");
        assertThat(managed.getHomeCurrencySequence()).isEqualTo(3L);
        verify(walletUserRepository).findByIdForUpdate(7L); // locked, so the sequence check can't interleave
        verify(walletUserRepository, never()).save(any());
    }

    @Test
    void homeCurrency_olderPickFromSameClient_committingLate_isIgnored() {
        service.setHomeCurrency(Currency.USD, "tab-1", 2L);

        service.setHomeCurrency(Currency.EUR, "tab-1", 1L); // e.g. timed out on the client, committed after USD
        service.setHomeCurrency(Currency.EUR, "tab-1", 2L); // same pick again (a retry)

        assertThat(managed.getHomeCurrency()).isEqualTo(Currency.USD);
        assertThat(managed.getHomeCurrencySequence()).isEqualTo(2L);
    }

    @Test
    void homeCurrency_fromAnotherClient_orWithoutOrdering_alwaysWins() {
        service.setHomeCurrency(Currency.USD, "tab-1", 5L);

        service.setHomeCurrency(Currency.EUR, "tab-2", 1L); // another tab's counter starts over
        assertThat(managed.getHomeCurrency()).isEqualTo(Currency.EUR);

        service.setHomeCurrency(Currency.TRY, null, null);
        assertThat(managed.getHomeCurrency()).isEqualTo(Currency.TRY);
        assertThat(managed.getHomeCurrencyClientId()).isNull();
        assertThat(managed.getHomeCurrencySequence()).isNull();
    }

    @Test
    void homeCurrency_withoutLoggedInUser_isNull_notAnError() {
        // disabled-security: the request is anonymous, GET /home/enums must still answer
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(service.getHomeCurrency()).isNull();
        // POST /home/currency must not fail either: there is just nothing to save it on
        service.setHomeCurrency(Currency.EUR, "tab-1", 1L);
        verifyNoInteractions(walletUserRepository);
    }

    @Test
    void homeCurrency_withNonWalletUserPrincipal_isLoadedByUsername() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", null, List.of()));
        managed.setHomeCurrency(Currency.EUR);
        when(walletUserRepository.findByUsername("alice")).thenReturn(Optional.of(managed));

        assertThat(service.getHomeCurrency()).isEqualTo(Currency.EUR);
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

    // Regression for F9: a password change left every existing session valid
    @Test
    void changePassword_revokesAllSessions() throws Exception {
        when(passwordEncoder.matches("current", "hash")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newHash");
        UserToken access = UserToken.builder().token("a").build();
        UserToken refresh = UserToken.builder().token("r").build();
        when(userTokenRepository.findAllNotFullyRevokedTokensByUserId(7L)).thenReturn(List.of(access, refresh));

        service.changePassword(ChangePasswordRequest.builder().currentPassword("current").newPassword("New1!pass").confirmPassword("New1!pass").build());

        assertThat(List.of(access, refresh)).noneMatch(UserToken::isActive);
    }

    @Test
    void changePassword_rejected_keepsSessions() {
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(ChangePasswordRequest.builder().currentPassword("wrong").newPassword("New1!pass").confirmPassword("New1!pass").build()))
                .isInstanceOf(MtgWalletGenericException.class);
        verify(userTokenRepository, never()).findAllNotFullyRevokedTokensByUserId(anyLong());
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
        managed.setHomeCurrency(Currency.EUR);
        managed.setHomeCurrencyClientId("tab-1");
        managed.setHomeCurrencySequence(4L);
        UserToken session = UserToken.builder().token("t").revoked(false).expired(false).build();
        when(userTokenRepository.findAllNotFullyRevokedTokensByUserId(7L)).thenReturn(List.of(session));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        service.anonymizeCurrentUser();

        assertThat(managed.getUsername()).startsWith("DLT#");
        assertThat(managed.getEmail()).startsWith("DLT#");
        assertThat(managed.getPassword()).isEqualTo("random-hash");
        assertThat(managed.getTotpSecret()).isNull();
        assertThat(managed.getTotpEnabled()).isFalse();
        assertThat(managed.getHomeCurrency()).isNull();
        assertThat(managed.getHomeCurrencyClientId()).isNull();
        assertThat(managed.getHomeCurrencySequence()).isNull();
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.isExpired()).isTrue();
    }

    @Test
    void anonymize_twice_givesDifferentUsernames_soTheUniqueColumnDoesNotClash() {
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        service.anonymizeCurrentUser();
        String first = managed.getUsername();
        managed.setUsername("alice");

        service.anonymizeCurrentUser();

        assertThat(managed.getUsername()).isNotEqualTo(first).hasSize(15);
    }

    @Test
    void changePassword_confirmationMismatch_isRejected_andKeepsTheOldHash() {
        when(passwordEncoder.matches("current", "hash")).thenReturn(true);
        ChangePasswordRequest request = ChangePasswordRequest.builder().currentPassword("current").newPassword("New1!pass").confirmPassword("Other1!pass").build();

        assertThatThrownBy(() -> service.changePassword(request))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.PASSWORDS_MISMATCH);
        assertThat(managed.getPassword()).isEqualTo("hash");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void validateUserId_null_isRejectedAsMissing() {
        assertThatThrownBy(() -> service.validateUserIdIfItsTheCurrentUser(null))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.USER_ID_CANT_BE_NULL);
    }

    @Test
    void currentUserId_withoutAuthentication_throwsUserNotFound() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.getCurrentUserId())
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void currentUsername_isEmptyForAnonymousOrMissingAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(service.getCurrentLoggedInUsername()).isEmpty();

        SecurityContextHolder.clearContext();
        assertThat(service.getCurrentLoggedInUsername()).isEmpty();
    }

    @Test
    void currentUsername_isEmptyForUnauthenticatedToken() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", "pw")); // not authenticated

        assertThat(service.getCurrentLoggedInUsername()).isEmpty();
    }

    // A token whose username was since freed (deleted / anonymized account) must not resolve to anyone
    @Test
    void currentUserEntity_whenTheRowIsGone_throwsUserNotFound() {
        when(walletUserRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUserEntity())
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void defaultPayees_areSetOnTheManagedUser() {
        com.mtg.mtgwalletbe.entity.Payee expense = new com.mtg.mtgwalletbe.entity.Payee();
        com.mtg.mtgwalletbe.entity.Payee income = new com.mtg.mtgwalletbe.entity.Payee();

        service.setDefaultPayeeForExpense(expense);
        service.setDefaultPayeeForIncome(income);

        assertThat(managed.getDefaultPayeeForExpense()).isSameAs(expense);
        assertThat(managed.getDefaultPayeeForIncome()).isSameAs(income);
        assertThat(principal.getDefaultPayeeForExpense()).isNull(); // the detached principal is not the row
    }

    @Test
    void createRole_duplicateName_isRejected() {
        com.mtg.mtgwalletbe.service.dto.RoleDto role = new com.mtg.mtgwalletbe.service.dto.RoleDto();
        role.setName("ROLE_ADMIN");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(new com.mtg.mtgwalletbe.entity.Role()));

        assertThatThrownBy(() -> service.createRole(role))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.ROLE_NAME_ALREADY_EXISTS);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void addRoleToUser_addsTheRole() throws Exception {
        com.mtg.mtgwalletbe.entity.Role admin = new com.mtg.mtgwalletbe.entity.Role(3L, "ROLE_ADMIN");
        when(walletUserRepository.findByUsername("alice")).thenReturn(Optional.of(managed));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(admin));

        service.addRoleToUser("alice", "ROLE_ADMIN");

        assertThat(managed.getRoles()).containsExactly(admin);
        verify(walletUserRepository).save(managed);
    }

    @Test
    void addRoleToUser_unknownRole_isRejected() {
        when(walletUserRepository.findByUsername("alice")).thenReturn(Optional.of(managed));
        when(roleRepository.findByName("ROLE_NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addRoleToUser("alice", "ROLE_NOPE"))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.ROLE_NOT_FOUND);
        assertThat(managed.getRoles()).isEmpty();
    }

    @Test
    void addRoleToUser_unknownUser_throwsUserNotFound() {
        when(walletUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addRoleToUser("ghost", "ROLE_ADMIN"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }
}
