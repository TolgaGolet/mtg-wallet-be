package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;

import java.util.Optional;

public interface UserService {
    RoleDto createRole(RoleDto role) throws MtgWalletGenericException;

    void addRoleToUser(String username, String roleName) throws MtgWalletGenericException;

    WalletUserBasicDto getUserBasicInfo(String username);

    Optional<String> getCurrentLoggedInUsername();

    /**
     * Id of the authenticated user, read from the security principal (no query).
     */
    Long getCurrentUserId();

    /**
     * Basic info of the authenticated user, read from the security principal (no query).
     */
    WalletUserBasicDto getCurrentLoggedInUser();

    /**
     * The authenticated user as a managed entity, for reading sensitive fields (password hash, TOTP secret) inside a
     * service transaction. Never return it from a controller.
     */
    WalletUser getCurrentUserEntity();

    void validateUserIdIfItsTheCurrentUser(Long userId) throws MtgWalletGenericException;


    void setDefaultPayeeForExpense(Payee payee);

    void setDefaultPayeeForIncome(Payee payee);

    void changePassword(ChangePasswordRequest request) throws MtgWalletGenericException;

    /**
     * Updates name, surname and email of the current user. A new email is marked unverified.
     *
     * @return true when the email changed
     */
    boolean updateProfile(String name, String surname, String email);

    void anonymizeCurrentUser();

    boolean emailExists(String email);
}
