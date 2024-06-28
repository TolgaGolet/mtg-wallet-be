package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountDto {
    private Long id;
    private Long userId;
    private String name;
    private AccountType type;
    private BigDecimal balance;
    private Currency currency;
}
