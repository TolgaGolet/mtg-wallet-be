package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUserResponse {
    private Long id;
    private String username;
    private String name;
    private String surname;
    private Collection<Role> roles = new ArrayList<>();
}
