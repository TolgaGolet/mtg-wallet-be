package com.mtg.mtgwalletbe.service.dto;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.Payee;
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
    private String email;
    private String name;
    private String surname;
    private String password;
    private Set<Role> roles = new HashSet<>();
    private List<Account> accounts = new ArrayList<>();
    private List<Payee> payees = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private Payee defaultPayeeForExpense;
    private Payee defaultPayeeForIncome;
    private Boolean isDefaultsCreated;
    private Boolean totpEnabled;
    private Boolean isEmailVerified;
    private String totpSecret;
}
