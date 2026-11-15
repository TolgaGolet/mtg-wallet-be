package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.TransactionCreateRequest;
import com.mtg.mtgwalletbe.api.request.TransactionUpdateRequest;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.Transaction;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.mapper.TransactionServiceMapper;
import com.mtg.mtgwalletbe.repository.TransactionRepository;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// Regression for CQ-06: new transactions only use ACTIVE accounts/payees; updates keep the existing ones usable
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceImplTest {

    @Mock private TransactionRepository repository;
    @Mock private TransactionServiceMapper mapper;
    @Mock private PayeeService payeeService;
    @Mock private AccountService accountService;
    @Mock private UserService userService;
    @Mock private CategoryService categoryService;
    @Mock private EntityManager entityManager;
    @InjectMocks private TransactionServiceImpl service;

    /** A balance change as AccountService.adjustBalance saw it: account id and the balance right after */
    private record BalanceChange(Long id, BigDecimal balance) {
    }

    // Every account starts at 1000; adjustBalance works on this ledger like the real service on managed rows
    private final Map<Long, BigDecimal> balances = new HashMap<>();
    private final List<BalanceChange> changes = new ArrayList<>();

    private static AccountDto account(long id) {
        return AccountDto.builder().id(id).balance(new BigDecimal("1000")).currency(Currency.TRY).build();
    }

    @BeforeEach
    void setUp() throws Exception {
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(payeeService.getPayee(anyLong())).thenAnswer(inv -> payee(inv.getArgument(0)));
        when(payeeService.getActivePayee(anyLong())).thenAnswer(inv -> payee(inv.getArgument(0)));
        when(accountService.getAccountById(anyLong())).thenAnswer(inv -> account(inv.getArgument(0)));
        when(accountService.getActiveAccountById(anyLong())).thenAnswer(inv -> account(inv.getArgument(0)));
        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            BigDecimal updated = balances.getOrDefault(id, new BigDecimal("1000")).add(inv.getArgument(1));
            balances.put(id, updated);
            changes.add(new BalanceChange(id, updated));
            return null;
        }).when(accountService).adjustBalance(anyLong(), any(BigDecimal.class));
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PayeeDto payee(long id) {
        return PayeeDto.builder().id(id).build();
    }

    @Test
    void create_usesActiveAccountAndPayeeOnly() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setTypeValue("EXP");
        request.setPayeeId(5L);
        request.setSourceAccountId(10L);
        request.setAmount(new BigDecimal("50"));
        request.setDateTime(LocalDateTime.now());

        service.create(request);

        verify(accountService).getActiveAccountById(10L);
        verify(payeeService).getActivePayee(5L);
        verify(accountService, never()).getAccountById(10L);
        verify(payeeService, never()).getPayee(anyLong());
    }

    private Transaction existingTransaction() {
        Account source = new Account();
        source.setId(10L);
        Payee payee = new Payee();
        payee.setId(5L);
        WalletUser user = WalletUser.builder().id(1L).build();
        Transaction transaction = new Transaction();
        transaction.setId(100L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("50"));
        transaction.setSourceAccount(source);
        transaction.setPayee(payee);
        transaction.setUser(user);
        return transaction;
    }

    private TransactionUpdateRequest updateRequest(long sourceAccountId, long payeeId) {
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setTypeValue("EXP");
        request.setPayeeId(payeeId);
        request.setSourceAccountId(sourceAccountId);
        request.setAmount(new BigDecimal("60"));
        request.setDateTime(LocalDateTime.now());
        return request;
    }

    @Test
    void update_keepingExistingAccountAndPayee_allowsThemEvenIfDeleted() throws Exception {
        when(repository.findById(100L)).thenReturn(Optional.of(existingTransaction()));

        service.update(updateRequest(10L, 5L), 100L);

        verify(accountService, never()).getActiveAccountById(anyLong());
        verify(payeeService, never()).getActivePayee(anyLong());
        verify(payeeService).getPayee(5L);
    }

    @Test
    void update_switchingToOtherAccountAndPayee_requiresActive() throws Exception {
        when(repository.findById(100L)).thenReturn(Optional.of(existingTransaction()));

        service.update(updateRequest(20L, 6L), 100L);

        verify(accountService).getActiveAccountById(20L);
        verify(payeeService).getActivePayee(6L);
    }

    // ---- CQ-05 / CQ-12: balance effects (every account starts at 1000 in these mocks) ----

    private List<BalanceChange> savedBalances() {
        return changes;
    }

    private TransactionCreateRequest createRequest(String type, Long targetAccountId) {
        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setTypeValue(type);
        request.setPayeeId(5L);
        request.setSourceAccountId(10L);
        request.setTargetAccountId(targetAccountId);
        request.setAmount(new BigDecimal("50"));
        request.setDateTime(LocalDateTime.now());
        return request;
    }

    @ParameterizedTest
    @CsvSource({"EXP, 950", "INC, 1050"})
    void create_appliesExpenseAndIncomeToSource(String type, String expectedBalance) throws Exception {
        service.create(createRequest(type, null));

        List<BalanceChange> saved = savedBalances();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).id()).isEqualTo(10L);
        assertThat(saved.get(0).balance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void create_transferMovesAmountFromSourceToTarget() throws Exception {
        service.create(createRequest("TRA", 11L));

        List<BalanceChange> saved = savedBalances();
        assertThat(saved).extracting(BalanceChange::id).containsExactly(10L, 11L);
        assertThat(saved.get(0).balance()).isEqualByComparingTo("950");
        assertThat(saved.get(1).balance()).isEqualByComparingTo("1050");
    }

    // F3: associations are references (no copies)
    @Test
    void create_storesReferences() throws Exception {
        Account sourceRef = new Account();
        Account targetRef = new Account();
        when(entityManager.getReference(Account.class, 10L)).thenReturn(sourceRef);
        when(entityManager.getReference(Account.class, 11L)).thenReturn(targetRef);
        org.mockito.ArgumentCaptor<Transaction> saved = org.mockito.ArgumentCaptor.forClass(Transaction.class);

        service.create(createRequest("TRA", 11L));

        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSourceAccount()).isSameAs(sourceRef);
        assertThat(saved.getValue().getTargetAccount()).isSameAs(targetRef);
        verify(accountService, never()).update(any(), anyLong());
    }

    @Test
    void create_transferToSameAccount_isRejected() {
        TransactionCreateRequest request = createRequest("TRA", 10L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(MtgWalletGenericException.class)
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.SOURCE_ACCOUNT_ID_CANT_BE_THE_SAME_AS_TARGET_ACCOUNT_ID);
    }

    @Test
    void create_transferBetweenCurrencies_isRejected() throws Exception {
        when(accountService.getActiveAccountById(11L)).thenReturn(AccountDto.builder().id(11L).balance(BigDecimal.ZERO).currency(Currency.USD).build());
        TransactionCreateRequest request = createRequest("TRA", 11L);

        assertThatThrownBy(() -> service.create(request))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.TARGET_ACCOUNT_CURRENCY_SHOULD_BE_THE_SAME_AS_SOURCE_ACCOUNT_CURRENCY);
    }

    @Test
    void create_expenseWithTarget_isRejected() {
        TransactionCreateRequest request = createRequest("EXP", 11L);

        assertThatThrownBy(() -> service.create(request))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES);
        verifyNoInteractions(repository);
    }

    @Test
    void update_rollsBackOldEffectThenAppliesNewOne() throws Exception {
        when(repository.findById(100L)).thenReturn(Optional.of(existingTransaction())); // EXPENSE 50 on account 10

        service.update(updateRequest(10L, 5L), 100L); // EXPENSE 60 on account 10

        List<BalanceChange> saved = savedBalances();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).balance()).isEqualByComparingTo("1050"); // rollback: +50
        assertThat(saved.get(1).balance()).isEqualByComparingTo("990");  // apply: -60 on top of the rollback
    }

    @Test
    void delete_transfer_reversesBothAccounts() throws Exception {
        Transaction transfer = existingTransaction();
        transfer.setType(TransactionType.TRANSFER);
        Account target = new Account();
        target.setId(11L);
        transfer.setTargetAccount(target);
        when(repository.findById(100L)).thenReturn(Optional.of(transfer));

        service.delete(100L);

        List<BalanceChange> saved = savedBalances();
        assertThat(saved).extracting(BalanceChange::id).containsExactly(10L, 11L);
        assertThat(saved.get(0).balance()).isEqualByComparingTo("1050");
        assertThat(saved.get(1).balance()).isEqualByComparingTo("950");
        verify(repository).delete(transfer);
    }

    // Regression: legacy TRANSFER rows without a target threw NPE on delete/update; the old code skipped the target
    @Test
    void delete_transferWithoutTarget_rollsBackSourceOnly() throws Exception {
        Transaction transfer = existingTransaction();
        transfer.setType(TransactionType.TRANSFER);
        when(repository.findById(100L)).thenReturn(Optional.of(transfer));

        service.delete(100L);

        List<BalanceChange> saved = savedBalances();
        assertThat(saved).extracting(BalanceChange::id).containsExactly(10L);
        assertThat(saved.get(0).balance()).isEqualByComparingTo("1050");
        verify(repository).delete(transfer);
    }
}
