package com.mtg.mtgwalletbe.security;

import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecurityParams {
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String REGISTER_PATH = "/auth/register";
    public static final String LOGIN_PATH = "/auth/authenticate";
    public static final String VERIFY_EMAIL_PATH = "/email-verification/verify/*";
    public static final String RESEND_VERIFY_EMAIL_PATH = "/email-verification/resend/*";
    public static final String TOTP_PATH = "/auth/totp/*";
    public static final String REFRESH_TOKEN_PATH = "/auth/refresh-token";
    public static final String LOGOUT_PATH = "/auth/logout";
    public static final String PASSWORD_RESET_REQUEST_PATH = "/auth/password-reset/request";
    public static final String PASSWORD_RESET_PATH = "/auth/password-reset/reset/*";
    public static final String ACCOUNT_RECOVERY_REQUEST_PATH = "/auth/account-recovery/request";
    public static final String ACCOUNT_RECOVERY_PATH = "/auth/account-recovery/recover/*";
    public static final String HEALTH_ACTUATOR_PATH = "/actuator/health";
    public static final String DATABASE_HEALTH_ACTUATOR_PATH = "/actuator/database-health";
    public static final String[] AUTH_WHITELIST = {
            LOGIN_PATH,
            REFRESH_TOKEN_PATH,
            REGISTER_PATH,
            HEALTH_ACTUATOR_PATH,
            DATABASE_HEALTH_ACTUATOR_PATH,
            VERIFY_EMAIL_PATH,
            RESEND_VERIFY_EMAIL_PATH,
            PASSWORD_RESET_REQUEST_PATH,
            PASSWORD_RESET_PATH,
            ACCOUNT_RECOVERY_REQUEST_PATH,
            ACCOUNT_RECOVERY_PATH,
            TOTP_PATH
    };
    public static final List<String> CORS_ALLOWED_ORIGINS = new ArrayList<>(Arrays.asList(
            "http://localhost:3000",
            "https://test-mtgwallet.onrender.com",
            "https://mtgwallet.onrender.com"
    ));
    @Value("${mtgWallet.applicationConfig.defaultPageSize}")
    public static final int DEFAULT_PAGE_SIZE = 10;

    private SecurityParams() {
        throw new IllegalStateException("SecurityParams class");
    }
}
