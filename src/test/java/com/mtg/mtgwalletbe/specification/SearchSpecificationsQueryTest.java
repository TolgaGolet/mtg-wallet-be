package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.api.request.CategorySearchRequest;
import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.api.request.TransactionSearchRequest;
import com.mtg.mtgwalletbe.entity.*;
import com.mtg.mtgwalletbe.enums.*;
import com.mtg.mtgwalletbe.repository.AccountRepository;
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// The search specifications run as real queries (offline H2, PostgreSQL mode), so the filters are checked by the rows
// they return, not by mocked Criteria calls. Each test rolls back.
@SpringBootTest
@ActiveProfiles("unittest")
@Transactional
class SearchSpecificationsQueryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PayeeRepository payeeRepository;
    @Autowired private TransactionRepository transactionRepository;

    private WalletUser alice;
    private WalletUser bob;

    @BeforeEach
    void setUp() {
        alice = user("alice");
        bob = user("bob");
    }

    // --- fixtures ---

    private WalletUser user(String username) {
        WalletUser user = WalletUser.builder().username(username).email(username + "@example.com").name(username)
                .password("hash").isDefaultsCreated(true).isEmailVerified(true).totpEnabled(false).build();
        entityManager.persist(user);
        return user;
    }

    private Account account(WalletUser owner, String name, AccountType type, Currency currency, Status status) {
        return accountRepository.save(Account.builder().user(owner).name(name).type(type)
                .balance(BigDecimal.ZERO).currency(currency).status(status).build());
    }

    private Account account(WalletUser owner, String name) {
        return account(owner, name, AccountType.CASH, Currency.TRY, Status.ACTIVE);
    }

    private Category category(WalletUser owner, String name, TransactionType type, Category parent, Status status) {
        Category category = new Category();
        category.setUser(owner);
        category.setName(name);
        category.setTransactionType(type);
        category.setParentCategory(parent);
        category.setIsParent(false);
        category.setStatus(status);
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

    private Transaction transaction(WalletUser owner, TransactionType type, Account source, Account target, Payee payee,
                                    String amount, LocalDateTime at, String notes) {
        Transaction transaction = new Transaction();
        transaction.setUser(owner);
        transaction.setType(type);
        transaction.setSourceAccount(source);
        transaction.setTargetAccount(target);
        transaction.setPayee(payee);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDateTime(at);
        transaction.setNotes(notes);
        return transactionRepository.save(transaction);
    }

    private List<String> accountNames(AccountSearchRequest request, Status status) {
        entityManager.flush();
        return accountRepository.findAll(AccountSpecification.search(request, status, alice.getId())).stream().map(Account::getName).toList();
    }

    private List<String> categoryNames(CategorySearchRequest request, Status status) {
        entityManager.flush();
        return categoryRepository.findAll(CategorySpecification.search(request, status, alice.getId())).stream().map(Category::getName).toList();
    }

    private List<String> payeeNames(PayeeSearchRequest request, Status status) {
        entityManager.flush();
        return payeeRepository.findAll(PayeeSpecification.search(request, status, alice.getId())).stream().map(Payee::getName).toList();
    }

    private List<Long> transactionIds(TransactionSearchRequest request) {
        entityManager.flush();
        return transactionRepository.findAll(TransactionSpecification.search(request, alice.getId())).stream().map(Transaction::getId).toList();
    }

    // --- accounts ---

    @Test
    void accounts_emptyRequest_returnsOnlyTheUsersAccountsInTheStatus() {
        account(alice, "Cash");
        account(alice, "Old", AccountType.CASH, Currency.TRY, Status.DELETED);
        account(bob, "Bobs");

        assertThat(accountNames(new AccountSearchRequest(), Status.ACTIVE)).containsExactly("Cash");
        assertThat(accountNames(new AccountSearchRequest(), null)).containsExactlyInAnyOrder("Cash", "Old");
    }

    @Test
    void accounts_nameFilter_isCaseInsensitiveContains() {
        account(alice, "Main Bank");
        account(alice, "Wallet");

        assertThat(accountNames(AccountSearchRequest.builder().name("bAnK").build(), Status.ACTIVE)).containsExactly("Main Bank");
        assertThat(accountNames(AccountSearchRequest.builder().name("  ank ").build(), Status.ACTIVE)).containsExactly("Main Bank");
    }

    @Test
    void accounts_typeAndCurrencyFilters_combineWithAnd() {
        account(alice, "Cash TRY", AccountType.CASH, Currency.TRY, Status.ACTIVE);
        account(alice, "Cash USD", AccountType.CASH, Currency.USD, Status.ACTIVE);
        account(alice, "Card USD", AccountType.CREDIT_CARD, Currency.USD, Status.ACTIVE);

        assertThat(accountNames(AccountSearchRequest.builder().typeValue("CASH").currencyValue("USD").build(), Status.ACTIVE))
                .containsExactly("Cash USD");
    }

    // A user can't read another user's account even when asking for its id
    @Test
    void accounts_idOfAnotherUsersAccount_findsNothing() {
        Account bobs = account(bob, "Bobs");

        assertThat(accountNames(AccountSearchRequest.builder().id(bobs.getId()).build(), null)).isEmpty();
    }

    // --- categories ---

    @Test
    void categories_childrenOnly_returnsTopLevelCategories_andIgnoresParentId() {
        Category food = category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE);
        category(alice, "Groceries", TransactionType.EXPENSE, food, Status.ACTIVE);
        category(alice, "Salary", TransactionType.INCOME, null, Status.ACTIVE);

        CategorySearchRequest topLevel = CategorySearchRequest.builder().childrenOnly(true).parentCategoryId(food.getId()).build();

        assertThat(categoryNames(topLevel, Status.ACTIVE)).containsExactlyInAnyOrder("Food", "Salary");
    }

    @Test
    void categories_parentIdFilter_returnsItsChildren() {
        Category food = category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE);
        category(alice, "Groceries", TransactionType.EXPENSE, food, Status.ACTIVE);
        category(alice, "Restaurants", TransactionType.EXPENSE, food, Status.ACTIVE);
        category(alice, "Rent", TransactionType.EXPENSE, null, Status.ACTIVE);

        assertThat(categoryNames(CategorySearchRequest.builder().parentCategoryId(food.getId()).build(), Status.ACTIVE))
                .containsExactlyInAnyOrder("Groceries", "Restaurants");
    }

    @Test
    void categories_typeAndStatusFilters() {
        category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE);
        category(alice, "Salary", TransactionType.INCOME, null, Status.ACTIVE);
        category(alice, "Bonus", TransactionType.INCOME, null, Status.DELETED);
        category(bob, "Bob Salary", TransactionType.INCOME, null, Status.ACTIVE);

        assertThat(categoryNames(CategorySearchRequest.builder().transactionTypeValue("INC").build(), Status.ACTIVE)).containsExactly("Salary");
    }

    // --- payees ---

    @Test
    void payees_transactionTypeFilter_goesThroughTheCategory() {
        Category groceries = category(alice, "Groceries", TransactionType.EXPENSE, null, Status.ACTIVE);
        Category salary = category(alice, "Salary", TransactionType.INCOME, null, Status.ACTIVE);
        payee(alice, "Market", groceries, Status.ACTIVE);
        payee(alice, "Employer", salary, Status.ACTIVE);

        PayeeSearchRequest request = new PayeeSearchRequest();
        request.setTransactionTypeValue("INC");

        assertThat(payeeNames(request, Status.ACTIVE)).containsExactly("Employer");
    }

    @Test
    void payees_categoryAndNameFilters_andStatus() {
        Category groceries = category(alice, "Groceries", TransactionType.EXPENSE, null, Status.ACTIVE);
        Category fuel = category(alice, "Fuel", TransactionType.EXPENSE, null, Status.ACTIVE);
        payee(alice, "Big Market", groceries, Status.ACTIVE);
        payee(alice, "Small Market", groceries, Status.DELETED);
        payee(alice, "Market Fuel", fuel, Status.ACTIVE);
        payee(bob, "Bobs Market", groceries, Status.ACTIVE);

        PayeeSearchRequest request = new PayeeSearchRequest();
        request.setCategoryId(groceries.getId());
        request.setName("market");

        assertThat(payeeNames(request, Status.ACTIVE)).containsExactly("Big Market");
        assertThat(payeeNames(request, null)).containsExactlyInAnyOrder("Big Market", "Small Market");
    }

    // --- transactions ---

    @Test
    void transactions_accountFilter_matchesSourceOrTarget() {
        Account cash = account(alice, "Cash");
        Account bank = account(alice, "Bank");
        Account card = account(alice, "Card");
        Payee payee = payee(alice, "Shop", category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE), Status.ACTIVE);
        LocalDateTime at = LocalDateTime.of(2026, 5, 1, 12, 0);
        Transaction fromCash = transaction(alice, TransactionType.EXPENSE, cash, null, payee, "10", at, null);
        Transaction bankToCash = transaction(alice, TransactionType.TRANSFER, bank, cash, payee, "20", at, null);
        transaction(alice, TransactionType.EXPENSE, card, null, payee, "30", at, null);

        TransactionSearchRequest request = new TransactionSearchRequest();
        request.setSourceAccountId(cash.getId());
        request.setTargetAccountId(cash.getId());

        assertThat(transactionIds(request)).containsExactlyInAnyOrder(fromCash.getId(), bankToCash.getId());
    }

    @Test
    void transactions_typeAmountAndPayeeFilters_combineWithAnd() {
        Account cash = account(alice, "Cash");
        Category food = category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE);
        Payee market = payee(alice, "Big Market", food, Status.ACTIVE);
        Payee cafe = payee(alice, "Cafe", food, Status.ACTIVE);
        LocalDateTime at = LocalDateTime.of(2026, 5, 1, 12, 0);
        Transaction match = transaction(alice, TransactionType.EXPENSE, cash, null, market, "12.50", at, null);
        transaction(alice, TransactionType.EXPENSE, cash, null, market, "99.00", at, null);
        transaction(alice, TransactionType.EXPENSE, cash, null, cafe, "12.50", at, null);
        transaction(alice, TransactionType.INCOME, cash, null, market, "12.50", at, null);

        TransactionSearchRequest request = new TransactionSearchRequest();
        request.setTypeValue("EXP");
        request.setAmount(new BigDecimal("12.5"));
        request.setPayeeName("MARKET");

        assertThat(transactionIds(request)).containsExactly(match.getId());
    }

    @Test
    void transactions_dateFilter_coversTheWholeCalendarDay_only() {
        Account cash = account(alice, "Cash");
        Payee payee = payee(alice, "Shop", category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE), Status.ACTIVE);
        Transaction startOfDay = transaction(alice, TransactionType.EXPENSE, cash, null, payee, "1", LocalDateTime.of(2026, 5, 1, 0, 0), null);
        Transaction endOfDay = transaction(alice, TransactionType.EXPENSE, cash, null, payee, "1", LocalDateTime.of(2026, 5, 1, 23, 59, 59), null);
        transaction(alice, TransactionType.EXPENSE, cash, null, payee, "1", LocalDateTime.of(2026, 4, 30, 23, 59, 59), null);
        transaction(alice, TransactionType.EXPENSE, cash, null, payee, "1", LocalDateTime.of(2026, 5, 2, 0, 0), null);

        TransactionSearchRequest request = new TransactionSearchRequest();
        request.setDateTime(LocalDateTime.of(2026, 5, 1, 15, 30)); // the time part is ignored

        assertThat(transactionIds(request)).containsExactlyInAnyOrder(startOfDay.getId(), endOfDay.getId());
    }

    @Test
    void transactions_ofOtherUsers_areNeverReturned() {
        Account bobsCash = account(bob, "Cash");
        Payee bobsPayee = payee(bob, "Shop", category(bob, "Food", TransactionType.EXPENSE, null, Status.ACTIVE), Status.ACTIVE);
        Transaction bobs = transaction(bob, TransactionType.EXPENSE, bobsCash, null, bobsPayee, "1", LocalDateTime.of(2026, 5, 1, 12, 0), null);

        TransactionSearchRequest byId = new TransactionSearchRequest();
        byId.setId(bobs.getId());

        assertThat(transactionIds(new TransactionSearchRequest())).isEmpty();
        assertThat(transactionIds(byId)).isEmpty();
    }

    // Regression: the input was lowercased with Turkish rules (I -> ı) but the column with the DB's lower() (I -> i),
    // so "Internet" searched "%ınternet%" and never found itself. Both sides are folded the same way now.
    @Test
    void accounts_nameFilter_findsANameContainingCapitalI_whenTypedExactlyAsStored() {
        account(alice, "Internet Bank");

        assertThat(accountNames(AccountSearchRequest.builder().name("Internet").build(), Status.ACTIVE)).containsExactly("Internet Bank");
    }

    // Every form of I matches every other: whatever keyboard the user types on
    @ParameterizedTest
    @ValueSource(strings = {"isparta", "ısparta", "ISPARTA", "İSPARTA", "Isparta"})
    void accounts_nameFilter_matchesAllFormsOfI(String typed) {
        account(alice, "Isparta Bank");

        assertThat(accountNames(AccountSearchRequest.builder().name(typed).build(), Status.ACTIVE)).containsExactly("Isparta Bank");
    }

    @Test
    void categories_nameFilter_findsDottedCapitalI_fromLowercaseInput() {
        category(alice, "İZMİR Trips", TransactionType.EXPENSE, null, Status.ACTIVE);

        assertThat(categoryNames(CategorySearchRequest.builder().name("izmir").build(), Status.ACTIVE)).containsExactly("İZMİR Trips");
    }

    @Test
    void payees_nameFilter_foldsTurkishCapitals() {
        Category food = category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE);
        payee(alice, "ŞEHİR ÇAY Cafe", food, Status.ACTIVE);
        payee(alice, "Other", food, Status.ACTIVE);
        PayeeSearchRequest request = new PayeeSearchRequest();
        request.setName("şehir çay");

        assertThat(payeeNames(request, Status.ACTIVE)).containsExactly("ŞEHİR ÇAY Cafe");
    }

    @Test
    void transactions_notesAndPayeeNameFilters_findCapitalI() {
        Account cash = account(alice, "Cash");
        Payee payee = payee(alice, "IKEA", category(alice, "Home", TransactionType.EXPENSE, null, Status.ACTIVE), Status.ACTIVE);
        LocalDateTime at = LocalDateTime.of(2026, 5, 1, 12, 0);
        Transaction match = transaction(alice, TransactionType.EXPENSE, cash, null, payee, "1", at, "Invoice INV-7");

        TransactionSearchRequest byNotes = new TransactionSearchRequest();
        byNotes.setNotes("invoice inv");
        TransactionSearchRequest byPayee = new TransactionSearchRequest();
        byPayee.setPayeeName("ikea");

        assertThat(transactionIds(byNotes)).containsExactly(match.getId());
        assertThat(transactionIds(byPayee)).containsExactly(match.getId());
    }

    // Folded in Java on write, so capitals the DB's lower() skips under the C locale still match
    @Test
    void accounts_nameFilter_matchesNonAsciiCapitals() {
        account(alice, "KÂR Market");
        account(alice, "ÉCOLE Fund");

        assertThat(accountNames(AccountSearchRequest.builder().name("kâr").build(), Status.ACTIVE)).containsExactly("KÂR Market");
        assertThat(accountNames(AccountSearchRequest.builder().name("école").build(), Status.ACTIVE)).containsExactly("ÉCOLE Fund");
    }

    // Decomposed input (I + U+0307) finds the precomposed İ that was stored
    @Test
    void accounts_nameFilter_matchesDecomposedInput() {
        account(alice, "İZMİR Bank");

        assertThat(accountNames(AccountSearchRequest.builder().name("İzmi̇r").build(), Status.ACTIVE)).containsExactly("İZMİR Bank");
    }

    // The escape character and the wildcards survive the fold and match literally
    @Test
    void accounts_nameFilter_matchesWildcardsAndBackslashLiterally() {
        account(alice, "50% Off \\ Store");
        account(alice, "500 Store");
        account(alice, "5x Store");

        assertThat(accountNames(AccountSearchRequest.builder().name("50%").build(), Status.ACTIVE)).containsExactly("50% Off \\ Store");
        assertThat(accountNames(AccountSearchRequest.builder().name("\\ st").build(), Status.ACTIVE)).containsExactly("50% Off \\ Store");
        assertThat(accountNames(AccountSearchRequest.builder().name("5_").build(), Status.ACTIVE)).isEmpty();
    }

    // Blank input means no filter: rows without notes must not disappear
    @Test
    void transactions_blankNotesFilter_keepsTransactionsWithoutNotes() {
        Account cash = account(alice, "Cash");
        Payee payee = payee(alice, "Shop", category(alice, "Food", TransactionType.EXPENSE, null, Status.ACTIVE), Status.ACTIVE);
        LocalDateTime at = LocalDateTime.of(2026, 5, 1, 12, 0);
        Transaction withNotes = transaction(alice, TransactionType.EXPENSE, cash, null, payee, "1", at, "lunch");
        Transaction withoutNotes = transaction(alice, TransactionType.EXPENSE, cash, null, payee, "1", at, null);
        TransactionSearchRequest blank = new TransactionSearchRequest();
        blank.setNotes("   ");

        assertThat(transactionIds(blank)).containsExactlyInAnyOrder(withNotes.getId(), withoutNotes.getId());
    }

    // @PreUpdate keeps the search column in step when a managed entity is renamed
    @Test
    void accounts_renamed_areFoundByTheNewNameOnly() {
        Account account = account(alice, "Old Name");
        entityManager.flush();
        account.setName("New Name");
        entityManager.flush();
        entityManager.clear();

        assertThat(accountNames(AccountSearchRequest.builder().name("new").build(), Status.ACTIVE)).containsExactly("New Name");
        assertThat(accountNames(AccountSearchRequest.builder().name("old").build(), Status.ACTIVE)).isEmpty();
    }
}
