package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Currency;
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

    /**
     * The currency the Home screen shows net value in, read from the security principal (no query when the JWT filter
     * loaded it). Null until set, or when no user is logged in (disabled-security).
     */
    Currency getHomeCurrency();

    /**
     * Saves the Home currency. With `clientId` and `sequence` (both or neither), a pick that is not newer than the last
     * one saved from the same client is ignored, so a request that commits late can't undo a newer pick. A no-op when
     * no user is logged in (disabled-security).
     */
    void setHomeCurrency(Currency currency, String clientId, Long sequence);

    /**
     * Changes the password and revokes every session of the user, including the current one.
     */
    void changePassword(ChangePasswordRequest request) throws MtgWalletGenericException;

    /**
     * Revokes all access and refresh tokens of the user (password change/reset, login, account deletion).
     */
    void revokeAllSessions(Long userId);

    /**
     * Sets a new password on the managed user and revokes all their sessions: every password change goes through
     * here, so no path can keep old tokens alive.
     */
    void replacePassword(WalletUser user, String rawPassword);

    /**
     * Updates name, surname and email of the current user. A new email is marked unverified.
     *
     * @return true when the email changed
     */
    boolean updateProfile(String name, String surname, String email);

    void anonymizeCurrentUser();

    boolean emailExists(String email);
}
