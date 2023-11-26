package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class WalletUser extends Auditable implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Size(min = 3, max = 15)
    @Column(unique = true)
    private String username;
    @NotNull
    @Size(min = 3, max = 15)
    private String name;
    @Size(max = 15)
    private String surname;
    @NotNull
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Role> roles = new HashSet<>();
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<UserToken> userTokens;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Account> accounts;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Payee> payees;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Category> categories;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_payee_id_for_expense")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payee defaultPayeeForExpense;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_payee_id_for_income")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payee defaultPayeeForIncome;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
