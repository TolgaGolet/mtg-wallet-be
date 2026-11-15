package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Currency;
import jakarta.persistence.AttributeConverter;
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
public class LenientCurrencyConverter implements AttributeConverter<Currency, String> {
    // WalletUser is loaded on every request and the stale value stays until the user picks again: warn once per value
    private static final Set<String> WARNED_VALUES = ConcurrentHashMap.newKeySet();
    private final CurrencyConverter strict = new CurrencyConverter();

    @Override
    public String convertToDatabaseColumn(Currency attribute) {
        return strict.convertToDatabaseColumn(attribute);
    }

    @Override
    public Currency convertToEntityAttribute(String dbData) {
        try {
            return strict.convertToEntityAttribute(dbData);
        } catch (IllegalArgumentException e) {
            if (WARNED_VALUES.add(dbData)) {
                log.warn("Unknown stored currency '{}', read as null (logged once per value)", dbData);
            }
            return null;
        }
    }
}
