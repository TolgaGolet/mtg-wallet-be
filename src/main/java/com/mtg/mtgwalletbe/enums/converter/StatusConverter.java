package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Status;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Status, String> {
    @Override
    public String convertToDatabaseColumn(Status attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(Status.values()).filter(item -> Objects.equals(item.getValue(), dbData)).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
