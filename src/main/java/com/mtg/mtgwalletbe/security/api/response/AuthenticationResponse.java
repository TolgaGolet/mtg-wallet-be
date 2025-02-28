package com.mtg.mtgwalletbe.security.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    @JsonProperty("accessToken")
    private String accessToken;
    @JsonProperty("refreshToken")
    private String refreshToken;
    @JsonProperty("emailVerificationRequired")
    private boolean emailVerificationRequired;
    @JsonProperty("totpRequired")
    private boolean totpRequired;
}
