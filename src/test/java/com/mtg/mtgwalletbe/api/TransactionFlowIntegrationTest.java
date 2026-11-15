package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.entity.*;
import com.mtg.mtgwalletbe.enums.*;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.*;
import com.mtg.mtgwalletbe.security.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mtg.mtgwalletbe.api.request.TransactionWriteRequest.NEW_PAYEE_ID;
import static com.mtg.mtgwalletbe.security.SecurityParams.ERROR_CODE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F5: the money flows end to end against the database (H2), through the real security filter chain (JWT + UserToken
 * row), controller, service transaction, mapper and JSON. Not @Transactional on purpose: each request commits or rolls
 * back in its own transaction like in production, so the F1 rollback cases are observable. Each test uses its own
 * user, and @AfterEach deletes that user's rows.
 */
@SpringBootTest
@ActiveProfiles("unittest")
@Import(TransactionFlowIntegrationTest.FailingWriter.class)
class TransactionFlowIntegrationTest {
    private static final AtomicInteger USER_COUNTER = new AtomicInteger();

    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter springSecurityFilterChain;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private WalletUserRepository userRepository;
    @Autowired private UserTokenRepository userTokenRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PayeeRepository payeeRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private JwtService jwtService;
    @Autowired private FailingWriter failingWriter;

    private MockMvc mvc;
    private String bearer;
    private WalletUser user;
    private Account cash;
    private Account bank;
    private Category groceries;
    private Payee market;

    /**
     * Writes, then throws a checked business error: the global rollbackOn = ALL_EXCEPTIONS must undo the write.
     */
    static class FailingWriter {
        private final AccountRepository accounts;

        FailingWriter(AccountRepository accounts) {
            this.accounts = accounts;
        }

        @Transactional
        public void addToBalanceThenFail(Long accountId) throws MtgWalletGenericException {
            Account account = accounts.findById(accountId).orElseThrow();
            account.setBalance(account.getBalance().add(new BigDecimal("100")));
            accounts.saveAndFlush(account);
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
        }
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        String username = "flow" + USER_COUNTER.incrementAndGet();
        transactionTemplate.executeWithoutResult(status -> {
            user = userRepository.save(WalletUser.builder().username(username).email(username + "@example.com")
                    .name(username).password("hash").isDefaultsCreated(true).isEmailVerified(true).build());
            cash = account("Cash", "1000", Currency.TRY);
            bank = account("Bank", "500", Currency.TRY);
            Category food = category("Food", TransactionType.EXPENSE, null, true);
            groceries = category("Groceries", TransactionType.EXPENSE, food, false);
            market = payee("Market", groceries);
            String token = jwtService.generateToken(user);
            userTokenRepository.save(UserToken.builder().user(user).token(token).tokenType(TokenType.BEARER)
                    .expired(false).revoked(false).build());
            bearer = "Bearer " + token;
        });
    }

    @AfterEach
    void deleteUserData() {
        transactionTemplate.executeWithoutResult(status -> {
            for (String jpql : new String[]{
                    "delete from Transaction t where t.user.id = :id",
                    "delete from Payee p where p.user.id = :id",
                    "delete from Category c where c.user.id = :id and c.parentCategory is not null",
                    "delete from Category c where c.user.id = :id",
                    "delete from Account a where a.user.id = :id",
                    "delete from UserToken t where t.user.id = :id",
                    "delete from WalletUser u where u.id = :id"}) {
                entityManager.createQuery(jpql).setParameter("id", user.getId()).executeUpdate();
            }
        });
    }

    private Account account(String name, String balance, Currency currency) {
        return accountRepository.save(Account.builder().user(user).name(name).type(AccountType.CASH)
                .balance(new BigDecimal(balance)).currency(currency).status(Status.ACTIVE).build());
    }

    private Category category(String name, TransactionType type, Category parent, boolean isParent) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setTransactionType(type);
        category.setParentCategory(parent);
        category.setIsParent(isParent);
        category.setStatus(Status.ACTIVE);
        return categoryRepository.save(category);
    }

    private Payee payee(String name, Category category) {
        Payee payee = new Payee();
        payee.setUser(user);
        payee.setName(name);
        payee.setCategory(category);
        payee.setStatus(Status.ACTIVE);
        return payeeRepository.save(payee);
    }

    private static String body(String type, Object payeeId, String amount, Long sourceId, Long targetId) {
        return """
                {"typeValue":"%s","payeeId":%s,"payeeName":"New Shop","categoryId":null,"amount":%s,
                 "dateTime":"2026-11-01T10:00:00","sourceAccountId":%d,"targetAccountId":%s,"notes":"note"}
                """.formatted(type, payeeId, amount, sourceId, targetId);
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Long createTransaction(String type, String amount, Long sourceId, Long targetId) throws Exception {
        String response = send(post("/transaction/create"), body(type, market.getId(), amount, sourceId, targetId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(response, "$.transactionId").longValue();
    }

    private BigDecimal balance(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }

    // Regression: create saved an id-only category copy and mapped that same instance back, so categoryName was null
    @Test
    void createPayee_answersTheCategoryName() throws Exception {
        send(post("/payee/create"), """
                {"name":"Bakery","categoryId":%d}
                """.formatted(groceries.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(groceries.getId()))
                .andExpect(jsonPath("$.categoryName").value("Groceries"));
    }

    @Test
    void createExpense_lowersBalance_andAnswersTheResponseShape() throws Exception {
        send(post("/transaction/create"), body("EXP", market.getId(), "50.25", cash.getId(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").isNumber())
                .andExpect(jsonPath("$.transactionType.value").value("EXP"))
                .andExpect(jsonPath("$.payeeId").value(market.getId()))
                .andExpect(jsonPath("$.sourceAccountId").value(cash.getId()))
                .andExpect(jsonPath("$.sourceAccountNewBalance").doesNotExist()) // F6
                .andExpect(jsonPath("$.targetAccountNewBalance").doesNotExist());

        assertThat(balance(cash)).isEqualByComparingTo("949.75");
    }

    @Test
    void createIncome_raisesBalance() throws Exception {
        createTransaction("INC", "200", cash.getId(), null);

        assertThat(balance(cash)).isEqualByComparingTo("1200");
    }

    @Test
    void createTransfer_movesMoneyBetweenAccounts() throws Exception {
        createTransaction("TRA", "300", cash.getId(), bank.getId());

        assertThat(balance(cash)).isEqualByComparingTo("700");
        assertThat(balance(bank)).isEqualByComparingTo("800");
    }

    @Test
    void updateAmount_replacesTheOldEffect() throws Exception {
        Long id = createTransaction("EXP", "100", cash.getId(), null);

        send(put("/transaction/update/" + id), body("EXP", market.getId(), "40", cash.getId(), null)).andExpect(status().isOk());

        assertThat(balance(cash)).isEqualByComparingTo("960");
    }

    @Test
    void updateSourceAccount_restoresTheOldAccount() throws Exception {
        Long id = createTransaction("EXP", "100", cash.getId(), null);

        send(put("/transaction/update/" + id), body("EXP", market.getId(), "100", bank.getId(), null)).andExpect(status().isOk());

        assertThat(balance(cash)).isEqualByComparingTo("1000");
        assertThat(balance(bank)).isEqualByComparingTo("400");
    }

    @Test
    void updateExpenseToTransfer_appliesBothAccounts() throws Exception {
        Long id = createTransaction("EXP", "100", cash.getId(), null);

        send(put("/transaction/update/" + id), body("TRA", market.getId(), "100", cash.getId(), bank.getId())).andExpect(status().isOk());

        assertThat(balance(cash)).isEqualByComparingTo("900");
        assertThat(balance(bank)).isEqualByComparingTo("600");
    }

    @Test
    void delete_restoresBalances() throws Exception {
        Long id = createTransaction("TRA", "250", cash.getId(), bank.getId());

        mvc.perform(delete("/transaction/delete/" + id).header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());

        assertThat(balance(cash)).isEqualByComparingTo("1000");
        assertThat(balance(bank)).isEqualByComparingTo("500");
        assertThat(transactionRepository.findById(id)).isEmpty();
    }

    // F1 regression: a new payee used to be committed although the transaction was rejected
    @Test
    void createWithNewPayee_onDeletedAccount_savesNothing() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            Account deleted = accountRepository.findById(bank.getId()).orElseThrow();
            deleted.setStatus(Status.DELETED);
        });
        long payeesBefore = payeeRepository.countByUserIdAndStatus(user.getId(), Status.ACTIVE);

        send(post("/transaction/create"), body("EXP", NEW_PAYEE_ID, "10", bank.getId(), null))
                .andExpect(status().isNotFound())
                .andExpect(header().string(ERROR_CODE_HEADER, "ACCOUNT_NOT_FOUND"));

        assertThat(payeeRepository.countByUserIdAndStatus(user.getId(), Status.ACTIVE)).isEqualTo(payeesBefore);
    }

    // F1: the rule itself. A checked business exception thrown after a write rolls the write back
    @Test
    void checkedBusinessException_rollsBackEarlierWrites() {
        assertThatThrownBy(() -> failingWriter.addToBalanceThenFail(cash.getId()))
                .isInstanceOf(MtgWalletGenericException.class);

        assertThat(balance(cash)).isEqualByComparingTo("1000");
    }

    @Test
    void search_returnsNewestFirst_withTheParentCategoryName() throws Exception {
        Long first = createTransaction("EXP", "10", cash.getId(), null);
        Long second = createTransaction("EXP", "20", cash.getId(), null);

        send(post("/transaction/search").param("pageNo", "0"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                // same dateTime: the id breaks the tie, newest first
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[1].id").value(first))
                .andExpect(jsonPath("$.content[0].payee.category.name").value("Groceries"))
                // F11: was always null, so the frontend showed "Groceries" instead of "Groceries (Food)"
                .andExpect(jsonPath("$.content[0].payee.category.parentCategoryName").value("Food"))
                .andExpect(jsonPath("$.content[0].sourceAccount.balance").value(970))
                .andExpect(jsonPath("$.content[0].sourceAccountNewBalance").doesNotExist());
    }

    @Test
    void withoutToken_is401() throws Exception {
        mvc.perform(post("/transaction/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
