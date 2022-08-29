package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.TransactionType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Objects;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {
    @Override
    public String convertToDatabaseColumn(TransactionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getKey();
    }

    @Override
    public TransactionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(TransactionType.values()).filter(item -> Objects.equals(item.getKey(), dbData)).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
