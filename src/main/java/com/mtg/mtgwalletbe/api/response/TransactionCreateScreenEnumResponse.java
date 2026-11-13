package com.mtg.mtgwalletbe.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateScreenEnumResponse {
    private List<CategoryResponse> categoryList;
    private List<AccountResponse> accountList;
}
