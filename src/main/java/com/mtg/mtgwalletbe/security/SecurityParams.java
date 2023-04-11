package com.mtg.mtgwalletbe.security;

public class SecurityParams {
    public static final String JWT_ACCESS_TOKEN_KEY = "accessToken";
    public static final String JWT_REFRESH_TOKEN_KEY = "refreshToken";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String JWT_TOKEN_ROLES_CLAIM_KEY = "roles";
    public static final String JWT_TOKEN_USERNAME_CLAIM_KEY = "username";
    public static final String LOGIN_PATH = "login";
    public static final String REFRESH_TOKEN_PATH = "/token/refresh";
    public static final String[] AUTH_WHITELIST = {
            LOGIN_PATH,
            REFRESH_TOKEN_PATH,
            "/user/create"
    };

    private SecurityParams() {
        throw new IllegalStateException("SecurityParams class");
    }
}
