package com.mtg.mtgwalletbe.api.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
