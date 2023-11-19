package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PayeeCreateRequest {
    @NotNull
    @Size(min = 3, max = 50)
    private String name;
    @NotNull
    private Long categoryId;
    @Size(min = 3, max = 15)
    private String username;
}
