package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategorySearchRequest {
    private Long id;
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9\\sçğıöşü]+$")
    private String name;
    @Size(min = 1, max = 15)
    private String transactionTypeValue;
    private Long userId;
    private Long parentCategoryId;
    private boolean childrenOnly;
}
