package com.mtg.mtgwalletbe.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletUserBasicDto {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String surname;
    private Boolean isDefaultsCreated;
    private Boolean totpEnabled;
}
