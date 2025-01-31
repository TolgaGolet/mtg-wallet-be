package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import static com.mtg.mtgwalletbe.entity.WalletUser.PASSWORD_REGULAR_EXPRESSION;

@Data
@Builder
public class ChangePasswordRequest {
    @NotNull
    @Pattern(regexp = PASSWORD_REGULAR_EXPRESSION)
    private String currentPassword;
    @NotNull
    @Pattern(regexp = PASSWORD_REGULAR_EXPRESSION)
    private String newPassword;
    @NotNull
    @Pattern(regexp = PASSWORD_REGULAR_EXPRESSION)
    private String confirmPassword;
}
