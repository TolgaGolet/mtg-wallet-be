package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Interval;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class IntervalConverter implements AttributeConverter<Interval, String> {
    @Override
    public String convertToDatabaseColumn(Interval attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public Interval convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(Interval.values()).filter(item -> Objects.equals(item.getValue(), dbData)).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
