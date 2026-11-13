package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
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
public class CategoryUpdateRequest {
    @NotNull
    @Size(min = 3, max = 50)
    @Pattern(regexp = NAME_REGULAR_EXPRESSION)
    private String name;
    private Long parentCategoryId;
}
