package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.entity.Role;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;

@Data
public class WalletUserDto {
    private Long id;
    private String username;
    private String name;
    private String surname;
    private String password;
    private Collection<Role> roles = new ArrayList<>();
}
