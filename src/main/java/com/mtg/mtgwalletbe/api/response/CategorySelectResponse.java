package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySelectResponse {
    private String value;
    private String label;
    private Long parentCategoryId;
    private TransactionType transactionType;
}
