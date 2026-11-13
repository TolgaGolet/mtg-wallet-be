package com.mtg.mtgwalletbe.service;

/**
 * Per-user caps, shared by controllers and services (so nothing depends on *ServiceImpl classes for them).
 */
public final class DomainLimits {
    public static final int MAX_ACCOUNTS = 40;
    public static final int MAX_CATEGORIES = 200;
    public static final int MAX_PAYEES = 200;

    private DomainLimits() {
    }
}
