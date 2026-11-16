package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.util.SearchText;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@ToString
@Builder
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
    private WalletUser user;
    @NotNull
    @Size(min = 3, max = 50)
    @Column(length = 50)
    private String name;
    // fold(name), kept in step on every write; searches match against this column (see SearchText)
    @Column(length = 50, nullable = false)
    @Setter(AccessLevel.NONE)
    private String searchName;
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
    // Optimistic locking: stops concurrent writes from silently overwriting the
    // balance.
    // The column default fills existing rows when ddl-auto adds the column.
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0 not null")
    private Long version;

    @PrePersist
    @PreUpdate
    void refreshSearchText() {
        searchName = SearchText.fold(name);
    }
}
