package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.converter.LenientCurrencyConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity // username and email need no extra @Index: their unique constraints already create one
// Intent-based changes (currency, password, profile) write only their own columns, so concurrent ones don't undo each other
@DynamicUpdate
public class WalletUser extends Auditable implements UserDetails {
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 15;
    public static final String USERNAME_REGULAR_EXPRESSION = "^[a-zA-Z0-9]+$";
    public static final int EMAIL_MIN_LENGTH = 3;
    public static final int EMAIL_MAX_LENGTH = 100;
    public static final String EMAIL_REGULAR_EXPRESSION = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
    public static final int NAME_MIN_LENGTH = 3;
    public static final int NAME_MAX_LENGTH = 15;
    public static final int SURNAME_MAX_LENGTH = 15;
    public static final String PASSWORD_REGULAR_EXPRESSION = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,64}$";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH)
    @Column(unique = true, length = USERNAME_MAX_LENGTH)
    private String username;
    @NotNull
    @Size(min = EMAIL_MIN_LENGTH, max = EMAIL_MAX_LENGTH)
    @Column(unique = true, length = EMAIL_MAX_LENGTH)
    private String email;
    @NotNull
    @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
    @Column(length = NAME_MAX_LENGTH)
    private String name;
    @Size(max = SURNAME_MAX_LENGTH)
    private String surname;
    @NotNull
    @ToString.Exclude // never in logs
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<UserToken> userTokens;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Account> accounts;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Payee> payees;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Category> categories;
    // LAZY: never read in a request, so the JWT filter's per-request user load doesn't join these
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_payee_id_for_expense")
    @JsonIgnore
    @ToString.Exclude
    private Payee defaultPayeeForExpense;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_payee_id_for_income")
    @JsonIgnore
    @ToString.Exclude
    private Payee defaultPayeeForIncome;
    @NotNull
    private Boolean isDefaultsCreated;
    @NotNull
    private Boolean isEmailVerified;
    @NotNull
    private Boolean totpEnabled = false;
    @ToString.Exclude // never in logs
    private String totpSecret;
    @Column(length = 10)
    @Convert(converter = LenientCurrencyConverter.class) // a stale value must not break the per-request user load
    private Currency homeCurrency; // null until the user picks one
    // Which client (tab) saved homeCurrency and its pick counter, so a late-committing older pick is ignored
    @Column(length = 36)
    private String homeCurrencyClientId;
    private Long homeCurrencySequence;

    // TODO add columns and functionalities for isCredentialsNonExpired, isEnabled etc.

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
