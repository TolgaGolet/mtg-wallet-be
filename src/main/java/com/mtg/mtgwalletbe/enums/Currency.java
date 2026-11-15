package com.mtg.mtgwalletbe.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Currency implements CodedEnum {
    TRY("TRY", "Turkish Lira"),
    USD("USD", "US Dollar"),
    EUR("EUR", "Euro");

    private final String value;
    private final String label;

    @JsonCreator
    public static Currency of(@JsonProperty("value") String value) {
        return CodedEnum.fromValue(Currency.class, value);
    }
}
