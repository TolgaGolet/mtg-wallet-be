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
public enum RecurrenceType {
    WEEKLY("W", "Every week"),
    TWO_WEEKS("2W", "Every two weeks"),
    FOUR_WEEKS("4W", "Every four weeks"),
    MONTHLY("M", "Every month at the same day");

    private final String key;
    private final String value;

    @JsonCreator
    public static RecurrenceType of(@JsonProperty("key") String key) {
        return Arrays.stream(RecurrenceType.values()).filter(item -> Objects.equals(item.getKey(), key)).findFirst().orElseThrow(() -> new IllegalArgumentException("IllegalArgumentException with key: " + key));
    }
}
