package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUserResponse {
    private Long userId;
    private String username;
    private String name;
    private String surname;
    private Set<Role> roles = new HashSet<>();
    private List<Account> accounts = new ArrayList<>();
    private List<Payee> payees = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
}
