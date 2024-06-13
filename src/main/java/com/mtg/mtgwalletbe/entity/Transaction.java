package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Column(length = 10)
    private TransactionType type;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payee payee;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private LocalDateTime dateTime;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account sourceAccount;
    @NotNull
    private BigDecimal sourceAccountNewBalance;
    private BigDecimal targetAccountNewBalance;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account targetAccount;
    @Column(length = 50)
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WalletUser user;
}
