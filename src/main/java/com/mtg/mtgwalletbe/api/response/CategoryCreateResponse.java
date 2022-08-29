package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateResponse {
    private Long categoryId;
    private String name;
    private TransactionType transactionType;
    private String username;
    private Long parentCategoryId;
}
