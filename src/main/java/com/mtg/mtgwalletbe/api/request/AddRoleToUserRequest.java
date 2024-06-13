package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddRoleToUserRequest {
    @NotNull
    @Size(min = 3, max = 15)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String username;
    @NotNull
    @Size(min = 3, max = 15)
    private String roleName;
}
