package com.mtg.mtgwalletbe.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Currency {
    TRY("TRY", "Turkish Lira"),
    USD("USD", "US Dollar"),
    EUR("EUR", "Euro");

    private final String key;
    private final String value;

    @JsonCreator
    public static Currency of(@JsonProperty("key") String key) {
        return Arrays.stream(Currency.values()).filter(item -> Objects.equals(item.getKey(), key)).findFirst().orElseThrow(() -> new IllegalArgumentException("IllegalArgumentException with key: " + key));
    }
}
