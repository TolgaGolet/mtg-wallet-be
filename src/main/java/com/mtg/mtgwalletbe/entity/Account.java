package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(columnList = "user_id")
})
public class Account extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WalletUser user;
    @NotNull
    @Size(min = 3, max = 50)
    @Column(length = 50)
    private String name;
    @NotNull
    @Column(length = 10)
    private AccountType type;
    @NotNull
    private BigDecimal balance;
    @NotNull
    @Column(length = 10)
    private Currency currency;
    @NotNull
    @Column(length = 10)
    private Status status;
}