package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.mtg.mtgwalletbe.entity.Payee.PAYEE_NAME_REGULAR_EXPRESSION;

@Data
public class TransactionSearchRequest {
    private Long id;
    @Size(min = 1, max = 15)
    private String typeValue;
    private Long payeeId;
    @Size(max = 50)
    @Pattern(regexp = PAYEE_NAME_REGULAR_EXPRESSION)
    private String payeeName;
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;
    private LocalDateTime dateTime;
    private Long sourceAccountId;
    private Long targetAccountId;
    @Size(min = 1, max = 50)
    private String notes;
    private Long userId;
}
