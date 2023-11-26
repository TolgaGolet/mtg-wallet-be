package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletUserRequest {
    @NotNull
    @Size(min = 3, max = 15)
    private String username;
    @NotNull
    @Size(min = 3, max = 100)
    private String email;
    @NotNull
    @Size(min = 3, max = 15)
    private String name;
    private String surname;
    @NotNull
    @Size(min = 4, max = 30)
    private String password;
}
