package com.mtg.mtgwalletbe.enums;

import com.mtg.mtgwalletbe.exception.InvalidEnumValueException;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An enum with a short stored/sent {@code value} and a display {@code label}. Each enum keeps its own
 * {@code @JsonCreator of(value)} (Jackson needs one per type), which delegates to {@link #fromValue}.
 */
public interface CodedEnum {
    String getValue();

    String getLabel();

    /**
     * The constant whose value matches, or empty when none does.
     */
    static <E extends Enum<E> & CodedEnum> Optional<E> find(Class<E> type, String value) {
        return Stream.of(type.getEnumConstants()).filter(item -> Objects.equals(item.getValue(), value)).findFirst();
    }

    /**
     * For values sent by the client: an unknown one throws {@link InvalidEnumValueException} (answered with 400).
     */
    static <E extends Enum<E> & CodedEnum> E fromValue(Class<E> type, String value) {
        return find(type, value).orElseThrow(() -> new InvalidEnumValueException(value));
    }
}
