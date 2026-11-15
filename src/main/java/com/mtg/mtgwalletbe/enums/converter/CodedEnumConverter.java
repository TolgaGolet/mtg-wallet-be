package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.CodedEnum;
import jakarta.persistence.AttributeConverter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores a {@link CodedEnum} as its {@code value}. An unknown stored value is a server-side data problem, so it throws
 * a plain IllegalArgumentException (500), not InvalidEnumValueException (400, meant for client input); subclasses can
 * override {@link #onUnknown}.
 */
public abstract class CodedEnumConverter<E extends Enum<E> & CodedEnum> implements AttributeConverter<E, String> {
    private final Class<E> type;
    // Built once: converters run for every enum column of every loaded row
    private final Map<String, E> byValue;

    protected CodedEnumConverter(Class<E> type) {
        this.type = type;
        this.byValue = Stream.of(type.getEnumConstants())
                .collect(Collectors.toUnmodifiableMap(CodedEnum::getValue, Function.identity()));
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        E constant = byValue.get(dbData);
        return constant != null ? constant : onUnknown(dbData);
    }

    /**
     * A stored value no constant has.
     */
    protected E onUnknown(String dbData) {
        throw new IllegalArgumentException("Unknown stored " + type.getSimpleName() + " value: " + dbData);
    }
}
