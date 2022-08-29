package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.api.request.TransactionCreateRequest;
import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateAccountBalancesDto {
    TransactionType transactionType;
    AccountDto sourceAccountDto;
    AccountDto targetAccountDto;
    TransactionCreateRequest transactionCreateRequest;
}
