package com.mtg.mtgwalletbe.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Status implements CodedEnum {
    ACTIVE("A", "Active"),
    INACTIVE("I", "Inactive"),
    DELETED("D", "Deleted");

    private final String value;
    private final String label;

    @JsonCreator
    public static Status of(@JsonProperty("value") String value) {
        return CodedEnum.fromValue(Status.class, value);
    }
}
