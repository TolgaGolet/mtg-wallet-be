package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.RoleRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
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

    @Override
    public RoleDto createRole(RoleDto role) throws MtgWalletGenericException {
        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ROLE_NAME_ALREADY_EXISTS.getMessage());
        }
        return mapper.toRoleDto(roleRepository.save(mapper.toRoleEntity(role)));
    }

    @Override
    public void addRoleToUser(String username, String roleName) throws MtgWalletGenericException {
        WalletUser walletUser = walletUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ROLE_NOT_FOUND.getMessage()));
        walletUser.getRoles().add(role);
        walletUserRepository.save(walletUser);
    }

    @Override
    public WalletUserDto getUserFullInfo(String username) {
        WalletUser walletUser = walletUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        return mapper.toWalletUserDto(walletUser);
    }

    @Override
    public WalletUserBasicDto getUserBasicInfo(String username) {
        WalletUser walletUser = walletUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        return mapper.toWalletUserBasicDto(walletUser);
    }

    @Override
    public Optional<String> getCurrentLoggedInUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return !Objects.equals(authentication.getName(), "anonymousUser") ? Optional.ofNullable(authentication.getName()) : Optional.empty();
    }

    @Override
    public WalletUserBasicDto getCurrentLoggedInUser() {
        String username = getCurrentLoggedInUsername().orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        return getUserBasicInfo(username);
    }

    @Override
    public WalletUserDto getCurrentLoggedInUserFull() {
        String username = getCurrentLoggedInUsername().orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        return getUserFullInfo(username);
    }

    @Override
    public void validateUserIdIfItsTheCurrentUser(Long userId) throws MtgWalletGenericException {
        if (userId == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.USER_ID_CANT_BE_NULL.getMessage());
        }
        if (!userId.equals(getCurrentLoggedInUser().getId())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM.getMessage());
        }
    }

    @Override
    public WalletUserDto updateUser(WalletUserDto walletUserDto) throws MtgWalletGenericException {
        WalletUser walletUser = walletUserRepository.findByUsername(walletUserDto.getUsername()).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        mapper.updateWalletUserFromDto(walletUserDto, walletUser);
        return mapper.toWalletUserDto(walletUserRepository.save(walletUser));
    }

    @Override
    public void changePassword(ChangePasswordRequest request) throws MtgWalletGenericException {
        WalletUserDto walletUserDto = getCurrentLoggedInUserFull();

        // check if the current password is correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), walletUserDto.getPassword())) {
            throw new IllegalStateException(GenericExceptionMessages.WRONG_PASSWORD.getMessage());
        }
        // check if the two new passwords are the same
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalStateException(GenericExceptionMessages.PASSWORDS_MISMATCH.getMessage());
        }
        // update the password
        walletUserDto.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // save the new password
        updateUser(walletUserDto);
    }

    @Override
    @Transactional
    public void deleteUser(WalletUserDto walletUserDto) {
        // TODO: check
        String deletedUserString = ("DLT#" + UUID.randomUUID()).substring(0, 15);
        WalletUser walletUser = walletUserRepository.findByUsername(walletUserDto.getUsername()).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        walletUserDto.setEmail(deletedUserString);
        walletUserDto.setName(deletedUserString);
        walletUserDto.setSurname(deletedUserString);
        walletUserDto.setUsername(deletedUserString);
        mapper.updateWalletUserFromDto(walletUserDto, walletUser);
        mapper.toWalletUserDto(walletUserRepository.save(walletUser));
    }
}
