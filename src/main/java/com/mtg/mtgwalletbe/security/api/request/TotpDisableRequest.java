package com.mtg.mtgwalletbe.security.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.mtg.mtgwalletbe.security.service.TotpService.TOTP_REGULAR_EXPRESSION;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotpDisableRequest {
    @NotNull
    @Pattern(regexp = TOTP_REGULAR_EXPRESSION)
    private String code;
}
