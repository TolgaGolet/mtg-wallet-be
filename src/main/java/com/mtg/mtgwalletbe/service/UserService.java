package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;

import java.security.Principal;
import java.util.Optional;

public interface UserService {
    RoleDto createRole(RoleDto role) throws MtgWalletGenericException;

    void addRoleToUser(String username, String roleName) throws MtgWalletGenericException;

    WalletUserDto getUser(String username);

    public Optional<String> getCurrentLoggedInUsername();

    public WalletUserDto getCurrentLoggedInUser();

    public void validateUsernameIfItsTheCurrentUser(String username) throws MtgWalletGenericException;

    WalletUserDto updateUser(WalletUserDto walletUserDto) throws MtgWalletGenericException;

    void changePassword(ChangePasswordRequest request, Principal connectedUser);
}
