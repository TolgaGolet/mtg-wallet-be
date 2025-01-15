package com.mtg.mtgwalletbe.security.api.request;

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
public class PasswordResetRequest {
    public static final int PASSWORD_DECODED_MIN_LENGTH = 3;
    public static final int PASSWORD_DECODED_MAX_LENGTH = 30;

    @NotNull
    @Size(min = PASSWORD_DECODED_MIN_LENGTH, max = PASSWORD_DECODED_MAX_LENGTH)
    private String password;
}
