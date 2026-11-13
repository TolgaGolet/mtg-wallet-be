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
        @Index(columnList = "user_id")
})
public class UserToken extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Column(unique = true)
    private String token;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TokenType tokenType = TokenType.BEARER;
    private boolean revoked;
    private boolean expired;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WalletUser user;

    /**
     * Usable for authentication: neither expired nor revoked.
     */
    public boolean isActive() {
        return !expired && !revoked;
    }

    /**
     * Active and of the given kind, so a refresh token can't be used as an access token or the other way around.
     */
    public boolean isActiveAs(TokenType type) {
        return tokenType == type && isActive();
    }

    public void revoke() {
        this.expired = true;
        this.revoked = true;
    }
}
