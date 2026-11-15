package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.*;
import com.mtg.mtgwalletbe.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// F7: custom @Query methods were never executed by a test. Runs on the offline H2 database (PostgreSQL mode);
// each test rolls back. Not covered: the native UserTokenRepository.deleteOlderTokensOfUserByCount. H2 re-evaluates
// its COUNT/LIMIT subqueries row by row while deleting (PostgreSQL uses the statement snapshot), so on H2 the result
// depends on row order; it needs a PostgreSQL (Testcontainers) test.
@SpringBootTest
@ActiveProfiles("unittest")
@Transactional
class RepositoryQueriesTest {

    @Autowired private EntityManager entityManager;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PayeeRepository payeeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserTokenRepository userTokenRepository;

    private WalletUser alice;
    private WalletUser bob;

    private WalletUser user(String username) {
        WalletUser user = WalletUser.builder().username(username).email(username + "@example.com").name(username)
                .password("hash").isDefaultsCreated(true).isEmailVerified(true).totpEnabled(false).build();
        entityManager.persist(user);
        return user;
    }

    private Account account(WalletUser owner, String name, String balance, Currency currency, Status status) {
        return accountRepository.save(Account.builder().user(owner).name(name).type(AccountType.CASH)
                .balance(new BigDecimal(balance)).currency(currency).status(status).build());
    }

    private Category category(WalletUser owner, String name, TransactionType type) {
        Category category = new Category();
        category.setUser(owner);
        category.setName(name);
        category.setTransactionType(type);
        category.setIsParent(false);
        category.setStatus(Status.ACTIVE);
        return categoryRepository.save(category);
    }

    private Payee payee(WalletUser owner, String name, Category category, Status status) {
        Payee payee = new Payee();
        payee.setUser(owner);
        payee.setName(name);
        payee.setCategory(category);
        payee.setStatus(status);
        return payeeRepository.save(payee);
    }

    private void transaction(WalletUser owner, TransactionType type, Account source, Payee payee, String amount, LocalDateTime at) {
        Transaction transaction = new Transaction();
        transaction.setUser(owner);
        transaction.setType(type);
        transaction.setSourceAccount(source);
        transaction.setPayee(payee);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDateTime(at);
        transaction.setSourceAccountNewBalance(BigDecimal.ZERO);
        transactionRepository.save(transaction);
    }

    private UserToken token(WalletUser owner, String value, boolean revoked) {
        UserToken token = UserToken.builder().user(owner).token(value).tokenType(TokenType.BEARER).revoked(revoked).expired(revoked).build();
        return userTokenRepository.save(token);
    }

    @BeforeEach
    void setUp() {
        alice = user("alice");
        bob = user("bob");
    }

    @Test
    void sumBalance_onlyActiveAccountsOfTheUserInTheCurrency() {
        account(alice, "Cash", "100.50", Currency.TRY, Status.ACTIVE);
        account(alice, "Bank", "20", Currency.TRY, Status.ACTIVE);
        account(alice, "Old", "1000", Currency.TRY, Status.DELETED);
        account(alice, "Dollars", "7", Currency.USD, Status.ACTIVE);
        account(bob, "Cash", "999", Currency.TRY, Status.ACTIVE);

        assertThat(accountRepository.sumBalanceByUserIdAndStatusAndCurrency(alice.getId(), Status.ACTIVE, Currency.TRY)).isEqualByComparingTo("120.50");
        assertThat(accountRepository.sumBalanceByUserIdAndStatusAndCurrency(alice.getId(), Status.ACTIVE, Currency.EUR)).isEqualByComparingTo("0");
    }

    @Test
    void accountNames_excludeTheGivenId_otherUsers_andInactive() {
        Account cash = account(alice, "Cash", "0", Currency.TRY, Status.ACTIVE);
        account(alice, "Bank", "0", Currency.TRY, Status.ACTIVE);
        account(alice, "Old", "0", Currency.TRY, Status.DELETED);
        account(bob, "Bobs", "0", Currency.TRY, Status.ACTIVE);

        assertThat(accountRepository.findNamesByUserIdAndStatus(alice.getId(), Status.ACTIVE, cash.getId())).containsExactly("Bank");
        assertThat(accountRepository.findNamesByUserIdAndStatus(alice.getId(), Status.ACTIVE, -1L)).containsExactlyInAnyOrder("Cash", "Bank");
    }

    @Test
    void payeeNames_andCount_areScopedToUserAndStatus() {
        Category groceries = category(alice, "Groceries", TransactionType.EXPENSE);
        payee(alice, "Migros", groceries, Status.ACTIVE);
        payee(alice, "Gone", groceries, Status.DELETED);
        payee(bob, "BobPayee", category(bob, "Groceries", TransactionType.EXPENSE), Status.ACTIVE);

        assertThat(payeeRepository.findNamesByUserIdAndStatus(alice.getId(), Status.ACTIVE, -1L)).containsExactly("Migros");
        assertThat(payeeRepository.countByUserIdAndStatus(alice.getId(), Status.ACTIVE)).isEqualTo(1);
    }

    @Test
    void profitLoss_isIncomeMinusExpense_inRangeAndCurrency_ignoringTransfers() {
        Account lira = account(alice, "Cash", "0", Currency.TRY, Status.ACTIVE);
        Account dollars = account(alice, "Dollars", "0", Currency.USD, Status.ACTIVE);
        Payee payee = payee(alice, "Shop", category(alice, "Shop", TransactionType.EXPENSE), Status.ACTIVE);
        LocalDateTime day = LocalDateTime.of(2026, 11, 10, 12, 0);
        transaction(alice, TransactionType.INCOME, lira, payee, "500", day);
        transaction(alice, TransactionType.EXPENSE, lira, payee, "120", day.plusHours(1));
        transaction(alice, TransactionType.TRANSFER, lira, payee, "50", day.plusHours(2));
        transaction(alice, TransactionType.EXPENSE, dollars, payee, "9", day);
        transaction(alice, TransactionType.EXPENSE, lira, payee, "1000", day.minusMonths(2));

        BigDecimal result = transactionRepository.getProfitLossByUserIdAndDateIntervalAndCurrency(alice.getId(),
                        day.minusDays(1), day.plusDays(1), Currency.TRY, TransactionType.EXPENSE,
                        List.of(TransactionType.EXPENSE, TransactionType.INCOME))
                .orElseThrow();

        assertThat(result).isEqualByComparingTo("380");
    }

    @Test
    void notFullyRevokedTokens_onlyActiveTokensOfTheUser() {
        UserToken active = token(alice, "a1", false);
        token(alice, "a2", true);
        token(bob, "b1", false);

        assertThat(userTokenRepository.findAllNotFullyRevokedTokensByUserId(alice.getId())).containsExactly(active);
    }
}
