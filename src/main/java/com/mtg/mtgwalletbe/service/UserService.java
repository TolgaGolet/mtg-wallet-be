package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;

public interface UserService {
    WalletUserDto saveUser(WalletUserDto walletUser) throws MtgWalletGenericException;

    RoleDto saveRole(RoleDto role) throws MtgWalletGenericException;

    void addRoleToUser(String username, String roleName) throws MtgWalletGenericException;

    WalletUserDto getUser(String username);
}
