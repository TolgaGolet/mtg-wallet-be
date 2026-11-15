package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.AccountType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountTypeConverter extends CodedEnumConverter<AccountType> {
    public AccountTypeConverter() {
        super(AccountType.class);
    }
}
