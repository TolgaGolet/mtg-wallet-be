package com.mtg.mtgwalletbe.api.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class WalletUserRequest {
    @NotNull
    @Size(min = 3, max = 15)
    private String username;
    @NotNull
    @Size(min = 3, max = 15)
    private String name;
    private String surname;
    @NotNull
    @Size(min = 4, max = 30)
    private String password;
}
