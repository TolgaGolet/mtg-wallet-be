package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotNull
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9\\sçğıöşü]+$")
    private String name;
    @NotNull
    @Size(min = 1, max = 15)
    private String transactionTypeValue;
    private Long parentCategoryId;
}
