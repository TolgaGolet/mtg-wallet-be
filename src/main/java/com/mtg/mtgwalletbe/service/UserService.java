package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;

import java.util.Optional;

public interface UserService {
    RoleDto createRole(RoleDto role) throws MtgWalletGenericException;

    void addRoleToUser(String username, String roleName) throws MtgWalletGenericException;

    WalletUserDto getUserFullInfo(String username);

    WalletUserBasicDto getUserBasicInfo(String username);

    public Optional<String> getCurrentLoggedInUsername();

    public WalletUserBasicDto getCurrentLoggedInUser();

    public WalletUserDto getCurrentLoggedInUserFull();

    public void validateUserIdIfItsTheCurrentUser(Long userId) throws MtgWalletGenericException;

    WalletUserDto updateUser(WalletUserDto walletUserDto) throws MtgWalletGenericException;

    void changePassword(ChangePasswordRequest request) throws MtgWalletGenericException;

    void deleteUser(WalletUserDto walletUser);
}
