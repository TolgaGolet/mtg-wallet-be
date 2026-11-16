package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.util.SearchText;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(columnList = "user_id")
})
public class Payee extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Column(length = 50)
    private String name;
    // fold(name), kept in step on every write; searches match against this column (see SearchText)
    @Column(length = 50)
    @Setter(AccessLevel.NONE)
    private String searchName;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY) // search fetches it with an entity graph (PayeeRepository.findAll)
    @ToString.Exclude
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private WalletUser user;
    @NotNull
    @Column(length = 10)
    private Status status;

    @PrePersist
    @PreUpdate
    void refreshSearchText() {
        searchName = SearchText.fold(name);
    }
}
