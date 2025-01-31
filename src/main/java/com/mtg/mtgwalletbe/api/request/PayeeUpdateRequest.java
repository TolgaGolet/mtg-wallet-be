package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.mtg.mtgwalletbe.entity.Payee.PAYEE_NAME_REGULAR_EXPRESSION;

@Data
public class PayeeUpdateRequest {
    @NotNull
    @Size(min = 3, max = 50)
    @Pattern(regexp = PAYEE_NAME_REGULAR_EXPRESSION)
    private String name;
}
