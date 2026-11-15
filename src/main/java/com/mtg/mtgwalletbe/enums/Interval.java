package com.mtg.mtgwalletbe.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Interval implements CodedEnum {
    DAILY("D", "Daily"),
    WEEKLY("W", "Weekly"),
    MONTHLY("M", "Monthly"),
    YEARLY("Y", "Yearly");

    private final String value;
    private final String label;

    @JsonCreator
    public static Interval of(@JsonProperty("value") String value) {
        return CodedEnum.fromValue(Interval.class, value);
    }
}
