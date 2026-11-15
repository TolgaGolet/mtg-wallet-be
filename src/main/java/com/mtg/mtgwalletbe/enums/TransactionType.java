package com.mtg.mtgwalletbe.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TransactionType implements CodedEnum {
    INCOME("INC", "Income"),
    EXPENSE("EXP", "Expense"),
    TRANSFER("TRA", "Transfer");

    private final String value;
    private final String label;

    @JsonCreator
    public static TransactionType of(@JsonProperty("value") String value) {
        return CodedEnum.fromValue(TransactionType.class, value);
    }
}
