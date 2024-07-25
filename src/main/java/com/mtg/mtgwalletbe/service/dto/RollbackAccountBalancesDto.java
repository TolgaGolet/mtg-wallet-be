package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RollbackAccountBalancesDto {
    private TransactionType transactionType;
    private AccountDto sourceAccountDto;
    private AccountDto targetAccountDto;
    private BigDecimal amount;
}
