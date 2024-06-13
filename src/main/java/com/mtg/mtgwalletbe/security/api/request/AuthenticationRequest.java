package com.mtg.mtgwalletbe.security.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    @NotNull
    @Size(min = 3, max = 15)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String username;
    @NotNull
    @Size(min = 4, max = 30)
    private String password;
}
