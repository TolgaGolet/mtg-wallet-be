package com.mtg.mtgwalletbe.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettingsDto {
    private String email;
    private String name;
    private String surname;
    private Boolean totpEnabled;
}
