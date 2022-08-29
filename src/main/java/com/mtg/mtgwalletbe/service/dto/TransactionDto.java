package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDto {
    private Long id;
    private TransactionType type;
    private PayeeDto payee;
    private BigDecimal amount;
    private LocalDateTime dateTime;
    private AccountDto sourceAccount;
    private BigDecimal sourceAccountNewBalance;
    private BigDecimal targetAccountNewBalance;
    private AccountDto targetAccount;
    private String notes;
    private WalletUserDto user;
}
