package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private TransactionType transactionType;
    private Boolean isParent;
    private Long parentCategoryId;
    private String parentCategoryName;
}
