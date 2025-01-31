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
public class TotpSetupResponse {
    @JsonProperty("qrCodeImage")
    private String qrCodeImage;
    @JsonProperty("secret")
    private String secret;
}
