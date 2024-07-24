package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private TransactionType transactionType;
    private Long userId;
    private Boolean isParent;
    private Long parentCategoryId;
    private String parentCategoryName;
    private Status status;
}
