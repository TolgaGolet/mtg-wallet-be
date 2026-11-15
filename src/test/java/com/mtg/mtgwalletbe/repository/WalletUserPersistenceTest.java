package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Currency;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// WalletUser is changed by small intent-based setters (@DynamicUpdate): one must not write back columns another changed
@SpringBootTest
@ActiveProfiles("unittest")
@Transactional
class WalletUserPersistenceTest {

    @Autowired private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        WalletUser user = WalletUser.builder().username("alice").email("alice@example.com").name("alice")
                .password("old-hash").isDefaultsCreated(true).isEmailVerified(true).totpEnabled(false).build();
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();
        userId = user.getId();
    }

    @Test
    void changingHomeCurrency_doesNotWriteBackOtherColumns() {
        WalletUser loaded = entityManager.find(WalletUser.class, userId);
        // Another transaction changes the password after this one loaded the row
        entityManager.createQuery("update WalletUser u set u.password = 'new-hash' where u.id = :id")
                .setParameter("id", userId).executeUpdate();

        loaded.setHomeCurrency(Currency.EUR);
        entityManager.flush();
        entityManager.clear();

        WalletUser reloaded = entityManager.find(WalletUser.class, userId);
        assertThat(reloaded.getHomeCurrency()).isEqualTo(Currency.EUR);
        assertThat(reloaded.getPassword()).isEqualTo("new-hash");
    }
}
