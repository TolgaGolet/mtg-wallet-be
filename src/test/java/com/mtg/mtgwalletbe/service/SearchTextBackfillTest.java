package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.entity.*;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// Rows written before the search columns existed have them NULL; the startup backfill fills them without touching
// auditing columns or @Version. The backfill's batches run in their own transactions, joining the test's one.
@SpringBootTest
@ActiveProfiles("unittest")
@Transactional
class SearchTextBackfillTest {

    @Autowired private EntityManager entityManager;
    @Autowired private SearchTextBackfill backfill;

    @Test
    void backfill_fillsMissingSearchColumns_withoutTouchingAuditOrVersion() {
        WalletUser user = WalletUser.builder().username("backfill").email("backfill@example.com").name("Backfill")
                .password("hash").isDefaultsCreated(true).isEmailVerified(true).totpEnabled(false).build();
        entityManager.persist(user);
        Account account = Account.builder().user(user).name("İZMİR Bank").type(AccountType.CASH)
                .balance(BigDecimal.ZERO).currency(Currency.TRY).status(Status.ACTIVE).build();
        entityManager.persist(account);
        Category category = new Category();
        category.setUser(user);
        category.setName("ÇAY");
        category.setTransactionType(TransactionType.EXPENSE);
        category.setIsParent(false);
        category.setStatus(Status.ACTIVE);
        entityManager.persist(category);
        Payee payee = new Payee();
        payee.setUser(user);
        payee.setName("KÂR Market");
        payee.setCategory(category);
        payee.setStatus(Status.ACTIVE);
        entityManager.persist(payee);
        Transaction withNotes = transaction(user, account, payee, "Invoice INV");
        Transaction withoutNotes = transaction(user, account, payee, null);
        entityManager.flush();
        for (String entity : new String[]{"Account", "Category", "Payee"}) {
            entityManager.createQuery("UPDATE " + entity + " e SET e.searchName = NULL").executeUpdate();
        }
        entityManager.createQuery("UPDATE Transaction t SET t.searchNotes = NULL").executeUpdate();
        entityManager.clear();
        Account before = entityManager.find(Account.class, account.getId());
        Long versionBefore = before.getVersion();
        LocalDateTime modifiedBefore = before.getLastModifiedDate();
        entityManager.clear();

        backfill.backfill();
        entityManager.clear();

        Account after = entityManager.find(Account.class, account.getId());
        assertThat(after.getSearchName()).isEqualTo("izmir bank");
        assertThat(after.getVersion()).isEqualTo(versionBefore);
        assertThat(after.getLastModifiedDate()).isEqualTo(modifiedBefore);
        assertThat(entityManager.find(Category.class, category.getId()).getSearchName()).isEqualTo("çay");
        assertThat(entityManager.find(Payee.class, payee.getId()).getSearchName()).isEqualTo("kâr market");
        assertThat(entityManager.find(Transaction.class, withNotes.getId()).getSearchNotes()).isEqualTo("invoice inv");
        assertThat(entityManager.find(Transaction.class, withoutNotes.getId()).getSearchNotes()).isNull();
    }

    // More rows than one batch: the loop keeps going until none are left
    @Test
    void backfill_coversMoreRowsThanOneBatch() {
        WalletUser user = WalletUser.builder().username("many").email("many@example.com").name("Many")
                .password("hash").isDefaultsCreated(true).isEmailVerified(true).totpEnabled(false).build();
        entityManager.persist(user);
        IntStream.range(0, 5).forEach(i -> entityManager.persist(Account.builder()
                .user(user).name("Acc " + i).type(AccountType.CASH).balance(BigDecimal.ZERO)
                .currency(Currency.TRY).status(Status.ACTIVE).build()));
        entityManager.flush();
        entityManager.createQuery("UPDATE Account a SET a.searchName = NULL").executeUpdate();
        entityManager.clear();

        backfill.setBatchSize(2); // 5 rows: batches of 2, 2, 1
        try {
            backfill.backfill();
        } finally {
            backfill.setBatchSize(500);
        }

        Long missing = entityManager.createQuery("SELECT COUNT(a) FROM Account a WHERE a.searchName IS NULL", Long.class)
                .getSingleResult();
        assertThat(missing).isZero();
    }

    private Transaction transaction(WalletUser user, Account account, Payee payee, String notes) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setSourceAccount(account);
        transaction.setPayee(payee);
        transaction.setAmount(BigDecimal.ONE);
        transaction.setDateTime(LocalDateTime.of(2026, 5, 1, 12, 0));
        transaction.setNotes(notes);
        entityManager.persist(transaction);
        return transaction;
    }
}
