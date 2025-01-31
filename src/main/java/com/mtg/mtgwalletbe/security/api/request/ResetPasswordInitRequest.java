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
public class ResetPasswordInitRequest {
    @NotNull
    @Size(min = EMAIL_MIN_LENGTH, max = EMAIL_MAX_LENGTH)
    @Pattern(regexp = EMAIL_REGULAR_EXPRESSION)
    private String email;
}
