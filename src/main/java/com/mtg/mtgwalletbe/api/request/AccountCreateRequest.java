package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountCreateRequest {
    @NotNull
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9\\sçğıöşü]+$")
    private String name;
    @NotNull
    @Size(min = 1, max = 15)
    private String typeValue;
    @NotNull
    @Digits(integer = 16, fraction = 2)
    private BigDecimal balance;
    @NotNull
    @Size(min = 1, max = 3)
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currencyValue;
}
