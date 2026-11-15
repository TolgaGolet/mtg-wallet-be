package com.mtg.mtgwalletbe.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AccountType implements CodedEnum {
    DEBIT_CARD("DEBIT", "Debit Card"),
    CREDIT_CARD("CREDIT", "Credit Card"),
    INVESTING_RETIREMENT("INVESTING", "Investing/Retirement"),
    CASH("CASH", "Cash");

    private final String value;
    private final String label;

    @JsonCreator
    public static AccountType of(@JsonProperty("value") String value) {
        return CodedEnum.fromValue(AccountType.class, value);
    }
}
