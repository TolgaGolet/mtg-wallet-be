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

    private Transaction existingTransfer() {
        Transaction transfer = existingTransaction();
        transfer.setType(TransactionType.TRANSFER);
        Account target = new Account();
        target.setId(11L);
        transfer.setTargetAccount(target);
        return transfer;
    }

    // Transaction 100 belongs to user 2, so the ownership check fails
    private void givenTransactionOfAnotherUser() throws Exception {
        Transaction foreign = existingTransaction();
        foreign.setUser(WalletUser.builder().id(2L).build());
        when(repository.findById(100L)).thenReturn(Optional.of(foreign));
        doThrow(new MtgWalletGenericException(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM))
                .when(userService).validateUserIdIfItsTheCurrentUser(2L);
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
        Transaction transfer = existingTransfer();
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

    // payeeId -1 (TransactionWriteRequest.NEW_PAYEE_ID): the frontend's "new payee" option
    @Test
    void create_withNewPayeeId_createsThePayeeFromNameAndCategory() throws Exception {
        TransactionCreateRequest request = createRequest("EXP", null);
        request.setPayeeId(com.mtg.mtgwalletbe.api.request.TransactionWriteRequest.NEW_PAYEE_ID);
        request.setPayeeName("New Shop");
        request.setCategoryId(7L);
        when(payeeService.create(any())).thenReturn(payee(55L));
        org.mockito.ArgumentCaptor<com.mtg.mtgwalletbe.api.request.PayeeCreateRequest> payeeRequest =
                org.mockito.ArgumentCaptor.forClass(com.mtg.mtgwalletbe.api.request.PayeeCreateRequest.class);

        service.create(request);

        verify(payeeService).create(payeeRequest.capture());
        assertThat(payeeRequest.getValue().getName()).isEqualTo("New Shop");
        assertThat(payeeRequest.getValue().getCategoryId()).isEqualTo(7L);
        verify(entityManager).getReference(Payee.class, 55L);
        verify(payeeService, never()).getActivePayee(anyLong());
    }

    // Validation runs before the payee is created, so a rejected transaction leaves no orphan payee behind
    @Test
    void create_invalidTransfer_withNewPayee_createsNoPayee() throws Exception {
        TransactionCreateRequest request = createRequest("TRA", 10L); // same account
        request.setPayeeId(com.mtg.mtgwalletbe.api.request.TransactionWriteRequest.NEW_PAYEE_ID);
        request.setPayeeName("New Shop");

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(MtgWalletGenericException.class);
        verify(payeeService, never()).create(any());
        assertThat(changes).isEmpty();
    }

    @Test
    void create_transferWithoutTarget_isRejected() {
        TransactionCreateRequest request = createRequest("TRA", null);

        assertThatThrownBy(() -> service.create(request))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
        assertThat(changes).isEmpty();
    }

    @Test
    void create_withDeletedSourceAccount_isRejected_withoutBalanceChanges() throws Exception {
        when(accountService.getActiveAccountById(10L)).thenThrow(new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND));

        assertThatThrownBy(() -> service.create(createRequest("EXP", null)))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
        assertThat(changes).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void update_missingTransaction_isNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(updateRequest(10L, 5L), 404L))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.TRANSACTION_NOT_FOUND);
    }

    @Test
    void update_ofOtherUsersTransaction_isRejected_withoutBalanceChanges() throws Exception {
        givenTransactionOfAnotherUser();

        assertThatThrownBy(() -> service.update(updateRequest(10L, 5L), 100L))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM);
        assertThat(changes).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void delete_ofOtherUsersTransaction_isRejected_withoutBalanceChanges() throws Exception {
        givenTransactionOfAnotherUser();

        assertThatThrownBy(() -> service.delete(100L)).isInstanceOf(MtgWalletGenericException.class);
        assertThat(changes).isEmpty();
        verify(repository, never()).delete(any(Transaction.class));
    }

    // Changing a transfer into an expense must give the old target its money back
    @Test
    void update_transferToExpense_rollsBackBothOldAccounts_thenAppliesToSourceOnly() throws Exception {
        Transaction transfer = existingTransfer(); // 50 from 10 to 11
        when(repository.findById(100L)).thenReturn(Optional.of(transfer));

        service.update(updateRequest(10L, 5L), 100L); // EXPENSE 60 from 10

        assertThat(balances.get(10L)).isEqualByComparingTo("990");  // +50 rollback, -60 apply
        assertThat(balances.get(11L)).isEqualByComparingTo("950");  // -50 rollback of the old transfer-in
        assertThat(transfer.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(transfer.getTargetAccount()).isNull();
    }

    @Test
    void update_toTheOldDeletedTargetAccount_isAllowed_butANewOneMustBeActive() throws Exception {
        Transaction transfer = existingTransfer();
        when(repository.findById(100L)).thenReturn(Optional.of(transfer));
        TransactionUpdateRequest request = updateRequest(10L, 5L);
        request.setTypeValue("TRA");
        request.setTargetAccountId(11L);

        service.update(request, 100L);

        verify(accountService).getAccountById(11L);
        verify(accountService, never()).getActiveAccountById(11L);
    }

    @Test
    void profitLoss_missingArguments_areRejected() {
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> service.getProfitLossByCurrentUserAndDateIntervalAndCurrency(null, now, Currency.TRY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getProfitLossByCurrentUserAndDateIntervalAndCurrency(now, null, Currency.TRY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getProfitLossByCurrentUserAndDateIntervalAndCurrency(now, now, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void profitLoss_withoutTransactions_isZero_notNull() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 1, 0, 0);
        when(repository.getProfitLossByUserIdAndDateIntervalAndCurrency(eq(1L), eq(start), eq(end), eq(Currency.TRY), any(), any()))
                .thenReturn(Optional.empty());

        assertThat(service.getProfitLossByCurrentUserAndDateIntervalAndCurrency(start, end, Currency.TRY)).isEqualByComparingTo("0");
    }

    @Test
    void profitLoss_countsExpenseAsTheNegativeType_andExcludesTransfers() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 1, 0, 0);
        when(repository.getProfitLossByUserIdAndDateIntervalAndCurrency(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new BigDecimal("-12.50")));

        assertThat(service.getProfitLossByCurrentUserAndDateIntervalAndCurrency(start, end, Currency.TRY)).isEqualByComparingTo("-12.50");
        verify(repository).getProfitLossByUserIdAndDateIntervalAndCurrency(1L, start, end, Currency.TRY,
                TransactionType.EXPENSE, List.of(TransactionType.EXPENSE, TransactionType.INCOME));
    }

    @Test
    void createScreenEnums_listActiveCategoriesAndAccounts() {
        var category = new com.mtg.mtgwalletbe.api.response.CategoryResponse();
        var account = new com.mtg.mtgwalletbe.api.response.AccountResponse();
        when(categoryService.search(any(), eq(com.mtg.mtgwalletbe.enums.Status.ACTIVE), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(category)));
        when(accountService.search(any(), eq(com.mtg.mtgwalletbe.enums.Status.ACTIVE), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(account)));

        var enums = service.getTransactionCreateScreenEnums();

        assertThat(enums.getCategoryList()).containsExactly(category);
        assertThat(enums.getAccountList()).containsExactly(account);
    }
}
