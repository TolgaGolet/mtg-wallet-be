package com.mtg.mtgwalletbe.security.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.mtg.mtgwalletbe.entity.WalletUser.*;
import static com.mtg.mtgwalletbe.security.service.TotpService.TOTP_REGULAR_EXPRESSION;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotpVerificationRequest {
    @NotNull
    @Pattern(regexp = TOTP_REGULAR_EXPRESSION)
    private String verificationCode;
    @NotNull
    @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH)
    @Pattern(regexp = USERNAME_REGULAR_EXPRESSION)
    private String username;
    @NotNull
    @Pattern(regexp = PASSWORD_REGULAR_EXPRESSION)
    private String password;
}
