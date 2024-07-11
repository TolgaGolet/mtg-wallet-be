package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateResponse {
    private Long transactionId;
    private TransactionType transactionType;
    private Long payeeId;
    private BigDecimal amount;
    private LocalDateTime dateTime;
    private Long sourceAccountId;
    private BigDecimal sourceAccountNewBalance;
    private BigDecimal targetAccountNewBalance;
    private Long targetAccountId;
    private String notes;
}
