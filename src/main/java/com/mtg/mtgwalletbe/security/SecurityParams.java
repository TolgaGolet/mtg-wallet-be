package com.mtg.mtgwalletbe.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecurityParams {
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String REGISTER_PATH = "/auth/register";
    public static final String LOGIN_PATH = "/auth/authenticate";
    public static final String REFRESH_TOKEN_PATH = "/auth/refresh-token";
    public static final String LOGOUT_PATH = "/auth/logout";
    public static final String[] AUTH_WHITELIST = {
            LOGIN_PATH,
            REFRESH_TOKEN_PATH,
            REGISTER_PATH
    };
    public static final List<String> CORS_ALLOWED_ORIGINS = new ArrayList<>(Arrays.asList(
            "http://localhost:3000",
            "https://test-mtgwallet.onrender.com",
            "https://mtgwallet.onrender.com"
    ));

    private SecurityParams() {
        throw new IllegalStateException("SecurityParams class");
    }
}
