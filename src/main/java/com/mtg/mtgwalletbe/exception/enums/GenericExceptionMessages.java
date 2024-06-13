package com.mtg.mtgwalletbe.exception.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * Returned HTTP status codes for exceptions are managed from here
 * com.mtg.mtgwalletbe.exception.MtgWalletExceptionHandler.handleException(com.mtg.mtgwalletbe.exception.MtgWalletGenericException)
 */
@Getter
@AllArgsConstructor
public enum GenericExceptionMessages {
    AUTHORIZATION_HEADER_MISSING("Authorization header is missing"),
    JWT_SUBJECT_MISSING("Token subject is missing"),
    JWT_NOT_VALID("Token is not valid"),
    JWT_EXPIRED("Token is expired"),
    BAD_USERNAME_OR_PASSWORD("Username or password is wrong"),
    USER_NOT_FOUND("User not found"),
    ROLE_NOT_FOUND("Role not found"),
    USERNAME_ALREADY_EXISTS("Username is already taken. Please choose another username"),
    USERNAME_CANT_BE_NULL("Username can't be null"),
    EMAIL_ALREADY_EXISTS("Email is already registered. Please choose another email address"),
    SYSTEM_USERNAME_NOT_ALLOWED("System username cannot be used"),
    ROLE_NAME_ALREADY_EXISTS("Role name already exists"),
    MISSING_REFRESH_TOKEN("Refresh token is missing"),
    PAYEE_NOT_FOUND("Payee not found"),
    ACCOUNT_NOT_FOUND("Account not found"),
    ACCOUNT_NAME_ALREADY_EXISTS("Account name already exists"),
    CATEGORY_NOT_FOUND("Category not found"),
    CATEGORY_NAME_ALREADY_EXISTS("Category name already exists"),
    PAYEE_NAME_ALREADY_EXISTS("Payee name already exists"),
    TARGET_ACCOUNT_ID_CANT_BE_EMPTY_FOR_TRANSFERS("Target account id can't be empty for transfer transactions"),
    TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES("Target account id should be empty for expense and income transactions"),
    SOURCE_ACCOUNT_ID_CANT_BE_THE_SAME_AS_TARGET_ACCOUNT_ID("Source account id can't be the same as target account id"),
    PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID("Payee category transaction type not valid"),
    NOT_AUTHORIZED_TO_PERFORM("User not authorized to perform this action");

    private final String message;

    public static GenericExceptionMessages fromMessage(String message) {
        for (GenericExceptionMessages value : values()) {
            if (value.getMessage().equals(message)) {
                return value;
            }
        }
        return null;
    }
}
