package com.mtg.mtgwalletbe.api.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class AccountCreateRequest {
    @NotNull
    @Size(min = 3, max = 15)
    private String username;
    @NotNull
    @Size(min = 3, max = 50)
    private String name;
    @NotNull
    @Size(min = 1, max = 15)
    private String typeKey;
    private BigDecimal balance;
    @NotNull
    @Size(min = 1, max = 3)
    private String currencyKey;
}
