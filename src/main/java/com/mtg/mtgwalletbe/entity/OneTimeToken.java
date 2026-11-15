package com.mtg.mtgwalletbe.entity;

import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Shared shape of the emailed single-use tokens (email verification, password reset, account recovery). Each subclass
 * keeps its own table; issuing and consuming go through OneTimeTokenService.
 */
@MappedSuperclass
@Getter
@Setter
@ToString
public abstract class OneTimeToken extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    @NotNull
    @Column(nullable = false, unique = true)
    @ToString.Exclude // a live credential
    private String token;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private WalletUser user;
    @NotNull
    private LocalDateTime expiryDate;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
