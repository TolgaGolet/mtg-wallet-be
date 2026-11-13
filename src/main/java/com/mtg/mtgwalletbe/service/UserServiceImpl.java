package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.RoleRepository;
import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserServiceMapper mapper;
    private final WalletUserRepository walletUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserTokenRepository userTokenRepository;

    @Override
    public RoleDto createRole(RoleDto role) throws MtgWalletGenericException {
        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ROLE_NAME_ALREADY_EXISTS);
        }
        return mapper.toRoleDto(roleRepository.save(mapper.toRoleEntity(role)));
    }

    @Override
    public void addRoleToUser(String username, String roleName) throws MtgWalletGenericException {
        WalletUser walletUser = findByUsername(username);
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ROLE_NOT_FOUND));
        walletUser.getRoles().add(role);
        walletUserRepository.save(walletUser);
    }

    @Override
    public WalletUserBasicDto getUserBasicInfo(String username) {
        return mapper.toWalletUserBasicDto(findByUsername(username));
    }

    @Override
    public Optional<String> getCurrentLoggedInUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return !Objects.equals(authentication.getName(), "anonymousUser") ? Optional.ofNullable(authentication.getName()) : Optional.empty();
    }

    /**
     * JwtAuthenticationFilter already loaded the user and stored it as the principal. Fall back to a lookup by
     * username only when the principal is something else.
     */
    private WalletUser principalOrLoad() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof WalletUser user) {
            return user;
        }
        String username = getCurrentLoggedInUsername().orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        return findByUsername(username);
    }

    @Override
    public Long getCurrentUserId() {
        return principalOrLoad().getId();
    }

    @Override
    public WalletUserBasicDto getCurrentLoggedInUser() {
        return mapper.toWalletUserBasicDto(principalOrLoad());
    }

    @Override
    public WalletUser getCurrentUserEntity() {
        return walletUserRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
    }

    @Override
    public void validateUserIdIfItsTheCurrentUser(Long userId) throws MtgWalletGenericException {
        if (userId == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.USER_ID_CANT_BE_NULL);
        }
        if (!userId.equals(getCurrentUserId())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM);
        }
    }

    @Override
    public void setDefaultPayeeForExpense(Payee payee) {
        getCurrentUserEntity().setDefaultPayeeForExpense(payee);
    }

    @Override
    public void setDefaultPayeeForIncome(Payee payee) {
        getCurrentUserEntity().setDefaultPayeeForIncome(payee);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) throws MtgWalletGenericException {
        WalletUser user = getCurrentUserEntity();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.WRONG_PASSWORD);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PASSWORDS_MISMATCH);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    public boolean updateProfile(String name, String surname, String email) {
        WalletUser user = getCurrentUserEntity();
        user.setName(name);
        user.setSurname(surname);
        boolean emailChanged = !Objects.equals(email, user.getEmail());
        if (emailChanged) {
            user.setEmail(email);
            user.setIsEmailVerified(Boolean.FALSE);
        }
        return emailChanged;
    }

    @Override
    public void anonymizeCurrentUser() {
        String deletedUserString = ("DLT#" + UUID.randomUUID()).substring(0, 15);
        WalletUser user = getCurrentUserEntity();
        user.setEmail(deletedUserString);
        user.setName(deletedUserString);
        user.setSurname(deletedUserString);
        user.setUsername(deletedUserString);
        user.setIsEmailVerified(Boolean.FALSE);
        // No credentials survive deletion: unguessable password, no 2FA secret, every session revoked
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setTotpSecret(null);
        user.setTotpEnabled(Boolean.FALSE);
        userTokenRepository.findAllNotFullyRevokedTokensByUserId(user.getId()).forEach(UserToken::revoke);
    }

    @Override
    public boolean emailExists(String email) {
        return walletUserRepository.existsByEmail(email);
    }

    private WalletUser findByUsername(String username) {
        return walletUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
    }
}
