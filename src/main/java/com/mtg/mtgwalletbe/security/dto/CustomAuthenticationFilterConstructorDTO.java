package com.mtg.mtgwalletbe.security.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.authentication.AuthenticationManager;

@Data
@Builder
public class CustomAuthenticationFilterConstructorDTO {
    private AuthenticationManager authenticationManager;
    private String jwtSecretKey;
    private int jwtAccessTokenExpirationDuration;
    private int jwtRefreshTokenExpirationDuration;
}
