package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidateTransactionDto {
    private PayeeDto payeeDto;
    private AccountDto sourceAccountDto;
    private AccountDto targetAccountDto;
    private TransactionType transactionType;
}
