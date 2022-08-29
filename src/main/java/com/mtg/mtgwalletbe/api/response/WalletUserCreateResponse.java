package com.mtg.mtgwalletbe.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUserCreateResponse {
    private Long userId;
    private String username;
    private String name;
    private String surname;
}
