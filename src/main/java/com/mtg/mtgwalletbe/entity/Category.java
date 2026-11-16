package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.util.SearchText;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(columnList = "parent_category_id, user_id"),
        @Index(columnList = "user_id")
})
public class Category extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Size(min = 3, max = 50)
    @Column(length = 50)
    private String name;
    // fold(name), kept in step on every write; searches match against this column (see SearchText)
    @Column(length = 50)
    @Setter(AccessLevel.NONE)
    private String searchName;
    @NotNull
    @Column(length = 10)
    private TransactionType transactionType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private WalletUser user;
    @NotNull
    private Boolean isParent;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    @JsonIgnore
    @ToString.Exclude
    private Category parentCategory;
    @NotNull
    @Column(length = 10)
    private Status status;

    @PrePersist
    @PreUpdate
    void refreshSearchText() {
        searchName = SearchText.fold(name);
    }
}
