package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.mtg.mtgwalletbe.api.request.ValidationPatterns.NAME_REGULAR_EXPRESSION;

@Data
public class AccountUpdateRequest {
    @NotNull
    @Size(min = 3, max = 50)
    @Pattern(regexp = NAME_REGULAR_EXPRESSION)
    private String name;
    @NotNull
    @Size(min = 1, max = 15)
    private String typeValue;
}
