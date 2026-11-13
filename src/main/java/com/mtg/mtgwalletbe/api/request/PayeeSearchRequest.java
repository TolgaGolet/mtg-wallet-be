package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.mtg.mtgwalletbe.api.request.ValidationPatterns.NAME_REGULAR_EXPRESSION;

@Data
public class PayeeSearchRequest {
    private Long id;
    @Size(max = 50)
    @Pattern(regexp = NAME_REGULAR_EXPRESSION)
    private String name;
    private Long categoryId;
    private Long userId;
    @Size(min = 1, max = 15)
    private String transactionTypeValue;
}
