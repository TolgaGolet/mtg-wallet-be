package com.mtg.mtgwalletbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import com.mtg.mtgwalletbe.enums.TokenType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "token")
})
public class UserToken extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Column(unique = true)
    public String token;
    @Enumerated(EnumType.STRING)
    public TokenType tokenType = TokenType.BEARER;
    public boolean revoked;
    public boolean expired;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    public WalletUser user;
}
