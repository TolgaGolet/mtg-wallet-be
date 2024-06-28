package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.service.dto.TransactionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailsResponse {
    private Long accountId;
    private String name;
    private AccountType type;
    private BigDecimal balance;
    private Currency currency;
    private Page<TransactionDto> transactions;
}
