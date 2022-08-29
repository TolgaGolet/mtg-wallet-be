package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;

import java.util.Optional;

public interface UserService {
    WalletUserDto createUser(WalletUserDto walletUser) throws MtgWalletGenericException;

    RoleDto createRole(RoleDto role) throws MtgWalletGenericException;

    void addRoleToUser(String username, String roleName) throws MtgWalletGenericException;

    WalletUserDto getUser(String username);

    public Optional<String> getCurrentLoggedInUsername();
}
