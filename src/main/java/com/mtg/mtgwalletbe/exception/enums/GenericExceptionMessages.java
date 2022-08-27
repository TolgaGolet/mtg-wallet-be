package com.mtg.mtgwalletbe.exception.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenericExceptionMessages {
    USER_NOT_FOUND("User not found"),
    ROLE_NOT_FOUND("Role not found"),
    USERNAME_ALREADY_EXISTS("Username already exist"),
    ROLE_NAME_ALREADY_EXISTS("Role name already exist"),
    MISSING_REFRESH_TOKEN("Refresh token is missing");

    private final String message;
}
