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
public enum Status {
    ACTIVE("A", "Active"),
    INACTIVE("I", "Inactive"),
    DELETED("D", "Deleted");

    private final String value;
    private final String label;

    @JsonCreator
    public static Status of(@JsonProperty("value") String value) {
        return Arrays.stream(Status.values()).filter(item -> Objects.equals(item.getValue(), value)).findFirst().orElseThrow(() -> new IllegalArgumentException("IllegalArgumentException with value: " + value));
    }
}
