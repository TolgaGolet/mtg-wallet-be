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
@Table(indexes = {
        @Index(columnList = "username"),
        @Index(columnList = "email")
})
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
    @Size(min = NAME_MIN_LENGTH, max = SURNAME_MAX_LENGTH)
    @Column(length = SURNAME_MAX_LENGTH)
    private String name;
    @Size(max = SURNAME_MAX_LENGTH)
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
    @NotNull
    private Boolean isDefaultsCreated;

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
