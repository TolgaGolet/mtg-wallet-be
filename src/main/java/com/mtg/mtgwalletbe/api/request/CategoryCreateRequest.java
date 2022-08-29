package com.mtg.mtgwalletbe.api.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
