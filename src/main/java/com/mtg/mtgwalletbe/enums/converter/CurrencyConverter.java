package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Currency;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CurrencyConverter extends CodedEnumConverter<Currency> {
    public CurrencyConverter() {
        super(Currency.class);
    }
}
