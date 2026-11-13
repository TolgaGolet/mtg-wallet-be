package com.mtg.mtgwalletbe.exception.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GenericExceptionMessages {
    AUTHORIZATION_HEADER_MISSING("Authorization header is missing", HttpStatus.UNAUTHORIZED),
    JWT_SUBJECT_MISSING("Token subject is missing", HttpStatus.UNAUTHORIZED),
    JWT_NOT_VALID("Token is not valid", HttpStatus.UNAUTHORIZED),
    JWT_EXPIRED("Token is expired", HttpStatus.UNAUTHORIZED),
    BAD_USERNAME_OR_PASSWORD("Username or password is wrong", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD("Wrong password", HttpStatus.BAD_REQUEST),
    PASSWORDS_MISMATCH("Passwords mismatch", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND("Role not found", HttpStatus.INTERNAL_SERVER_ERROR),
    USERNAME_ALREADY_EXISTS("Username is already taken. Please choose another username", HttpStatus.CONFLICT),
    USER_ID_CANT_BE_NULL("User ID can't be null", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_ALREADY_EXISTS("Email is already registered. Please choose another email address", HttpStatus.CONFLICT),
    SYSTEM_USERNAME_NOT_ALLOWED("System username cannot be used", HttpStatus.BAD_REQUEST),
    ROLE_NAME_ALREADY_EXISTS("Role name already exists", HttpStatus.CONFLICT),
    PAYEE_NOT_FOUND("Payee not found", HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_FOUND("Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_NAME_ALREADY_EXISTS("Account name already exists", HttpStatus.CONFLICT),
    ACCOUNTS_LIMIT_EXCEEDED("User accounts limit exceeded", HttpStatus.BAD_REQUEST),
    CATEGORIES_LIMIT_EXCEEDED("Categories limit exceeded", HttpStatus.BAD_REQUEST),
    PARENT_CATEGORY_CANT_HAVE_PARENT_CATEGORY("Parent category can't have a parent category", HttpStatus.BAD_REQUEST),
    CATEGORY_WITH_PARENT_CATEGORY_NOT_ALLOWED_AS_PARENT_CATEGORY("Category with parent category is not allowed as parent category", HttpStatus.BAD_REQUEST),
    PAYEES_LIMIT_EXCEEDED("Payees limit exceeded", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND("Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_ALREADY_EXISTS("Category name already exists", HttpStatus.CONFLICT),
    PAYEE_NAME_ALREADY_EXISTS("Payee name already exists", HttpStatus.CONFLICT),
    TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES("Target account id should be empty for expense and income transactions", HttpStatus.BAD_REQUEST),
    SOURCE_ACCOUNT_ID_CANT_BE_THE_SAME_AS_TARGET_ACCOUNT_ID("Source account id can't be the same as target account id", HttpStatus.BAD_REQUEST),
    TARGET_ACCOUNT_CURRENCY_SHOULD_BE_THE_SAME_AS_SOURCE_ACCOUNT_CURRENCY("Target account currency should be the same as source account currency", HttpStatus.BAD_REQUEST),
    PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID("Payee category transaction type not valid", HttpStatus.BAD_REQUEST),
    NOT_AUTHORIZED_TO_PERFORM("User not authorized to perform this action", HttpStatus.BAD_REQUEST),
    TRANSACTION_NOT_FOUND("Transaction not found", HttpStatus.NOT_FOUND),
    DATABASE_DOWN("Database is down", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_EMAIL_VERIFICATION_TOKEN("Invalid email verification token", HttpStatus.BAD_REQUEST),
    EXPIRED_EMAIL_VERIFICATION_TOKEN("Email verification token expired", HttpStatus.BAD_REQUEST),
    NOT_VERIFIED_EMAIL("Email is not verified", HttpStatus.BAD_REQUEST),
    ALREADY_VERIFIED_EMAIL("Email is already verified", HttpStatus.CONFLICT),
    INVALID_PASSWORD_RESET_TOKEN("Invalid password reset token", HttpStatus.BAD_REQUEST),
    EXPIRED_PASSWORD_RESET_TOKEN("Password reset token expired", HttpStatus.BAD_REQUEST),
    INVALID_ACCOUNT_RECOVERY_TOKEN("Invalid account recovery token", HttpStatus.BAD_REQUEST),
    EXPIRED_ACCOUNT_RECOVERY_TOKEN("Account recovery token expired", HttpStatus.BAD_REQUEST),
    INVALID_TOTP_CODE("Invalid TOTP code", HttpStatus.BAD_REQUEST),
    CATEGORY_CANT_BE_ITS_OWN_PARENT("Category can't be its own parent category", HttpStatus.BAD_REQUEST),
    PARENT_CATEGORY_TRANSACTION_TYPE_MISMATCH("Parent category must have the same transaction type", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_ACTIVE_SUBCATEGORIES("Category has subcategories. Move or delete them first", HttpStatus.CONFLICT);

    private final String message;
    // Returned by MtgWalletExceptionHandler for MtgWalletGenericException. 401/403 make the frontend refresh the token
    // and retry, so errors caused by user input (wrong password, TOTP code) must be 400.
    private final HttpStatus status;
}
