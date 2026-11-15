package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.TransactionType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionTypeConverter extends CodedEnumConverter<TransactionType> {
    public TransactionTypeConverter() {
        super(TransactionType.class);
    }
}
