package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateRequest {
    @NotNull
    @Size(min = 3, max = 15)
    private String name;
}
