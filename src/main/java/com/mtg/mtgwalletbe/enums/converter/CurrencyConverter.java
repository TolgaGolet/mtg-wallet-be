package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Currency;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Objects;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class CurrencyConverter implements AttributeConverter<Currency, String> {
    @Override
    public String convertToDatabaseColumn(Currency attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getKey();
    }

    @Override
    public Currency convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(Currency.values()).filter(item -> Objects.equals(item.getKey(), dbData)).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
