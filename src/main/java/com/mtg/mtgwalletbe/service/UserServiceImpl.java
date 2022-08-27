package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.RoleRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

// TODO check transactional usage and use
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserServiceMapper mapper;
    private final WalletUserRepository walletUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        WalletUser walletUser = walletUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        walletUser.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getName())));
        return new org.springframework.security.core.userdetails.User(walletUser.getUsername(), walletUser.getPassword(), authorities);
    }

    @Override
    public WalletUserDto saveUser(WalletUserDto walletUser) throws MtgWalletGenericException {
        log.info("Saving new user {} to the database.", walletUser.getUsername());
        if (walletUserRepository.findByUsername(walletUser.getUsername()).isPresent()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.USERNAME_ALREADY_EXISTS.getMessage());
        }
        walletUser.setPassword(passwordEncoder.encode(walletUser.getPassword()));
        return mapper.toWalletUserDto(walletUserRepository.save(mapper.toWalletUserEntity(walletUser)));
    }

    @Override
    public RoleDto saveRole(RoleDto role) throws MtgWalletGenericException {
        log.info("Saving new role {} to the database.", role.getName());
        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ROLE_NAME_ALREADY_EXISTS.getMessage());
        }
        return mapper.toRoleDto(roleRepository.save(mapper.toRoleEntity(role)));
    }

    @Override
    public void addRoleToUser(String username, String roleName) throws MtgWalletGenericException {
        log.info("Adding role {} to user {}.", roleName, username);
        WalletUser walletUser = walletUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ROLE_NOT_FOUND.getMessage()));
        walletUser.getRoles().add(role);
        walletUserRepository.save(walletUser);
    }

    @Override
    public WalletUserDto getUser(String username) {
        log.info("Fetching user {}", username);
        WalletUser walletUser = walletUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(GenericExceptionMessages.USER_NOT_FOUND.getMessage()));
        return mapper.toWalletUserDto(walletUser);
    }
}
