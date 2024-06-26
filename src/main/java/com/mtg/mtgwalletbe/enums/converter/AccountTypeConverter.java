package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.AccountType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class AccountTypeConverter implements AttributeConverter<AccountType, String> {
    @Override
    public String convertToDatabaseColumn(AccountType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public AccountType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(AccountType.values()).filter(item -> Objects.equals(item.getValue(), dbData)).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
