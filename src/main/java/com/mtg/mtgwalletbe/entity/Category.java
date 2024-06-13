package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Size(min = 3, max = 50)
    @Column(length = 50)
    private String name;
    @NotNull
    @Column(length = 10)
    private TransactionType transactionType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WalletUser user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category parentCategory;
}
