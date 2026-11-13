package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import static com.mtg.mtgwalletbe.api.request.ValidationPatterns.NAME_REGULAR_EXPRESSION;

@Data
@Builder
@NoArgsConstructor // Jackson 3 needs it to deserialize request bodies
@AllArgsConstructor
public class CategorySearchRequest {
    private Long id;
    @Size(min = 3, max = 50)
    @Pattern(regexp = NAME_REGULAR_EXPRESSION)
    private String name;
    @Size(min = 1, max = 15)
    private String transactionTypeValue;
    private Long userId;
    private Long parentCategoryId;
    private boolean childrenOnly;
}
