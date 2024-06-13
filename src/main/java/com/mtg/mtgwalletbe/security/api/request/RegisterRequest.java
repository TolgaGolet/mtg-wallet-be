package com.mtg.mtgwalletbe.security.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.mtg.mtgwalletbe.entity.WalletUser.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    public static final int PASSWORD_DECODED_MIN_LENGTH = 3;
    public static final int PASSWORD_DECODED_MAX_LENGTH = 30;

    @NotNull
    @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH)
    @Pattern(regexp = USERNAME_REGULAR_EXPRESSION)
    private String username;
    @NotNull
    @Size(min = EMAIL_MIN_LENGTH, max = EMAIL_MAX_LENGTH)
    @Pattern(regexp = EMAIL_REGULAR_EXPRESSION)
    private String email;
    @NotNull
    @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
    private String name;
    @Size(max = SURNAME_MAX_LENGTH)
    private String surname;
    @NotNull
    @Size(min = PASSWORD_DECODED_MIN_LENGTH, max = PASSWORD_DECODED_MAX_LENGTH)
    private String password;
}
