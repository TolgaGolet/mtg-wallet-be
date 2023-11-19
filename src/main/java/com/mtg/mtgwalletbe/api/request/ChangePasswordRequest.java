package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChangePasswordRequest {
    @NotNull
    @Size(min = 4, max = 30)
    private String currentPassword;
    @NotNull
    @Size(min = 4, max = 30)
    private String newPassword;
    @NotNull
    @Size(min = 4, max = 30)
    private String confirmationPassword;
}
