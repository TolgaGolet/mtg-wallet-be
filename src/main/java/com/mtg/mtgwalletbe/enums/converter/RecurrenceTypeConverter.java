package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.RecurrenceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class RecurrenceTypeConverter implements AttributeConverter<RecurrenceType, String> {
    @Override
    public String convertToDatabaseColumn(RecurrenceType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public RecurrenceType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(RecurrenceType.values()).filter(item -> Objects.equals(item.getValue(), dbData)).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
