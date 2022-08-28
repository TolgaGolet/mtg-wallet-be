package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.AccountType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Objects;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class AccountTypeConverter implements AttributeConverter<AccountType, String> {
    @Override
    public String convertToDatabaseColumn(AccountType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getKey();
    }

    @Override
    public AccountType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(AccountType.values()).filter(item -> Objects.equals(item.getKey(), dbData)).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
