package com.mtg.mtgwalletbe.api.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionCreateRequest {
    @NotNull
    @Size(min = 1, max = 15)
    private String typeKey;
    @NotNull
    private Long payeeId;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private LocalDateTime dateTime;
    @NotNull
    private Long sourceAccountId;
    private Long targetAccountId;
    @Size(min = 1, max = 50)
    private String notes;
    @NotNull
    @Size(min = 1, max = 15)
    private String username;
}
