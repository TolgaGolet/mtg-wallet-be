package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import static com.mtg.mtgwalletbe.entity.WalletUser.PASSWORD_REGULAR_EXPRESSION;
import static com.mtg.mtgwalletbe.security.service.TotpService.TOTP_REGULAR_EXPRESSION;

@Data
@Builder
@NoArgsConstructor // Jackson 3 needs it to deserialize request bodies
@AllArgsConstructor
public class DeleteAccountRequest {
    @NotNull
    @Pattern(regexp = PASSWORD_REGULAR_EXPRESSION)
    private String password;
    @Pattern(regexp = TOTP_REGULAR_EXPRESSION)
    private String verificationCode;
}
