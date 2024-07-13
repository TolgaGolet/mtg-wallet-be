package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountSearchRequest {
    private Long id;
    private Long userId;
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9\\sçğıöşü]+$")
    private String name;
    @Size(min = 1, max = 15)
    private String typeValue;
    @Size(min = 1, max = 15)
    private String currencyValue;
}
