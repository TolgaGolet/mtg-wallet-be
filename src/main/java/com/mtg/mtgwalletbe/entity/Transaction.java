package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.util.SearchText;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(columnList = "dateTime"),
        @Index(columnList = "source_account_id"),
        @Index(columnList = "target_account_id"),
        @Index(columnList = "user_id"),
        @Index(columnList = "user_id, dateTime")
})
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
    private Payee payee;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private LocalDateTime dateTime;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    @ToString.Exclude
    private Account sourceAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    @ToString.Exclude
    private Account targetAccount;
    @Column(length = 50)
    private String notes;
    // fold(notes), kept in step on every write; searches match against this column (see SearchText)
    @Column(length = 50)
    @Setter(AccessLevel.NONE)
    private String searchNotes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private WalletUser user;

    @PrePersist
    @PreUpdate
    void refreshSearchText() {
        searchNotes = SearchText.fold(notes);
    }
}
