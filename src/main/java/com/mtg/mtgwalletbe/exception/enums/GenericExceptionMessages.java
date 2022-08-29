package com.mtg.mtgwalletbe.exception.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenericExceptionMessages {
    USER_NOT_FOUND("User not found"),
    ROLE_NOT_FOUND("Role not found"),
    USERNAME_ALREADY_EXISTS("Username already exists"),
    ROLE_NAME_ALREADY_EXISTS("Role name already exists"),
    MISSING_REFRESH_TOKEN("Refresh token is missing"),
    PAYEE_NOT_FOUND("Payee not found"),
    ACCOUNT_NOT_FOUND("Account not found"),
    CATEGORY_NOT_FOUND("Category not found"),
    TARGET_ACCOUNT_ID_CANT_BE_EMPTY_FOR_TRANSFERS("Target account id can't be empty for transfer transactions"),
    TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES("Target account id should be empty for expense and income transactions"),
    SOURCE_ACCOUNT_ID_CANT_BE_THE_SAME_AS_TARGET_ACCOUNT_ID("Source account id can't be the same as target account id"),
    PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID("Payee category transaction type not valid");

    private final String message;
}
