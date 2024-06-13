package com.mtg.mtgwalletbe.security.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.mtg.mtgwalletbe.entity.WalletUser.*;
import static com.mtg.mtgwalletbe.security.api.request.RegisterRequest.PASSWORD_DECODED_MAX_LENGTH;
import static com.mtg.mtgwalletbe.security.api.request.RegisterRequest.PASSWORD_DECODED_MIN_LENGTH;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    @NotNull
    @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH)
    @Pattern(regexp = USERNAME_REGULAR_EXPRESSION)
    private String username;
    @NotNull
    @Size(min = PASSWORD_DECODED_MIN_LENGTH, max = PASSWORD_DECODED_MAX_LENGTH)
    private String password;
}
