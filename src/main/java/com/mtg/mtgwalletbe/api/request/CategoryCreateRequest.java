package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotNull
    @Size(min = 3, max = 50)
    private String name;
    @NotNull
    @Size(min = 1, max = 15)
    private String transactionTypeKey;
    @Size(min = 3, max = 15)
    private String username;
    private Long parentCategoryId;
}
