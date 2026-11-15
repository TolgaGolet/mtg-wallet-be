package com.mtg.mtgwalletbe.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletUserTest {

    // F7 regression: @Builder ignored the field initializers, so a freshly built user had roles == null
    @Test
    void builder_keepsFieldDefaults() {
        WalletUser user = WalletUser.builder().username("alice").build();

        assertThat(user.getRoles()).isNotNull().isEmpty();
        assertThat(user.getAuthorities()).isNotNull().isEmpty();
        assertThat(user.getTotpEnabled()).isFalse();
    }

    @Test
    void noArgsConstructor_keepsFieldDefaults() {
        WalletUser user = new WalletUser();

        assertThat(user.getRoles()).isNotNull().isEmpty();
        assertThat(user.getTotpEnabled()).isFalse();
    }
}
