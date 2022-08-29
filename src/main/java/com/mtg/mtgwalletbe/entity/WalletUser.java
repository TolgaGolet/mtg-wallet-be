package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUser extends Auditable {
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
}
