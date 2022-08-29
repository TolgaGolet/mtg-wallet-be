package com.mtg.mtgwalletbe.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayeeDto {
    private Long id;
    private String name;
    private CategoryDto category;
    private WalletUserDto user;
}
