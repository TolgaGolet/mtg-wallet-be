package com.mtg.mtgwalletbe.service.dto;

import lombok.Data;

@Data
public class WalletUserBasicDto {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String surname;
}
