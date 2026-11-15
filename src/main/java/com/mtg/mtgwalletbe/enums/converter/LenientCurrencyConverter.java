package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Currency;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads an unknown stored value as null instead of throwing. Not auto-applied: only for optional preferences on rows
 * that are loaded on every request (WalletUser), where a removed or renamed currency must not lock the user out.
 */
@Slf4j
@Converter
public class LenientCurrencyConverter extends CodedEnumConverter<Currency> {
    // WalletUser is loaded on every request and the stale value stays until the user picks again: warn once per value
    private static final Set<String> WARNED_VALUES = ConcurrentHashMap.newKeySet();

    public LenientCurrencyConverter() {
        super(Currency.class);
    }

    @Override
    protected Currency onUnknown(String dbData) {
        if (WARNED_VALUES.add(dbData)) {
            log.warn("Unknown stored currency '{}', read as null (logged once per value)", dbData);
        }
        return null;
    }
}
