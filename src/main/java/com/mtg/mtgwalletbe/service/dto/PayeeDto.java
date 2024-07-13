package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.enums.Status;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayeeDto {
    private Long id;
    private String name;
    private CategoryDto category;
    private Long userId;
    private Status status;
}
