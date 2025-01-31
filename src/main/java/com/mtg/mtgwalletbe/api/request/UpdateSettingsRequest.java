package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import static com.mtg.mtgwalletbe.entity.WalletUser.*;

@Data
@Builder
public class UpdateSettingsRequest {
    @NotNull
    @Size(min = EMAIL_MIN_LENGTH, max = EMAIL_MAX_LENGTH)
    @Pattern(regexp = EMAIL_REGULAR_EXPRESSION)
    private String email;
    @NotNull
    @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
    private String name;
    @NotNull
    @Size(max = SURNAME_MAX_LENGTH)
    private String surname;
}
