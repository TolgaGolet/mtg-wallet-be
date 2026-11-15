package com.mtg.mtgwalletbe.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Per-user caps, shared by controllers and services (so nothing depends on *ServiceImpl classes for them).
 */
public final class DomainLimits {
    public static final int MAX_ACCOUNTS = 40;
    public static final int MAX_CATEGORIES = 200;
    public static final int MAX_PAYEES = 200;

    private DomainLimits() {
    }

    /**
     * One page holding every row up to the cap, sorted by name: for dropdowns that list all of a user's items.
     */
    public static Pageable allByName(int limit) {
        return byName(0, limit);
    }

    /**
     * One page of a name-sorted list (account, category and payee searches).
     */
    public static Pageable byName(int pageNo, int size) {
        return PageRequest.of(pageNo, size, Sort.by("name").ascending());
    }
}
