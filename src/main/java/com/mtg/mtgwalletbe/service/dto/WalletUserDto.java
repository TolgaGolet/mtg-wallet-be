package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Role;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class WalletUserDto {
    private Long id;
    private String username;
    private String name;
    private String surname;
    private String password;
    private Set<Role> roles = new HashSet<>();
    private List<Account> accounts = new ArrayList<>();
}
