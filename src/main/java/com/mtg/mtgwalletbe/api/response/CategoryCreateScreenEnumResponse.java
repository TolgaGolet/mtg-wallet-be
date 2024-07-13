package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateScreenEnumResponse {
    private Set<TransactionType> transactionTypes = Set.of(TransactionType.values()).stream().filter(item -> item != TransactionType.TRANSFER).collect(Collectors.toSet());
    private List<CategorySelectResponse> parentCategoryList;
}
