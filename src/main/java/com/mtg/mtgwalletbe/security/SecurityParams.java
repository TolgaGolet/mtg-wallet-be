package com.mtg.mtgwalletbe.security;

import com.auth0.jwt.algorithms.Algorithm;

public class SecurityParams {
    public static final String JWT_SECRET_KEY = "#Hm$2nA89ZEvH$7j";
    public static final int JWT_ACCESS_TOKEN_EXPIRATION_DURATION = 10;
    public static final int JWT_REFRESH_TOKEN_EXPIRATION_DURATION = 30;
    public static final Algorithm JWT_SIGNING_ALGORITHM = Algorithm.HMAC256(JWT_SECRET_KEY.getBytes());
    public static final String JWT_ACCESS_TOKEN_KEY = "accessToken";
    public static final String JWT_REFRESH_TOKEN_KEY = "refreshToken";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String JWT_TOKEN_CLAIM_KEY = "roles";
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
