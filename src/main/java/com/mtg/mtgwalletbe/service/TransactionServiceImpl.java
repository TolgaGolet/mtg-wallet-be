package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.annotation.Loggable;
import com.mtg.mtgwalletbe.api.request.*;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.TransactionCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.Transaction;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.TransactionServiceMapper;
import com.mtg.mtgwalletbe.repository.TransactionRepository;
import com.mtg.mtgwalletbe.service.dto.*;
import com.mtg.mtgwalletbe.specification.TransactionSpecification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_ACCOUNTS;
import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_CATEGORIES;
import static com.mtg.mtgwalletbe.service.DomainLimits.allByName;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository repository;
    private final TransactionServiceMapper mapper;
    private final PayeeService payeeService;
    private final AccountService accountService;
    private final UserService userService;
    private final CategoryService categoryService;
    // References (no query) for associations: the services above already loaded and ownership-checked these rows,
    // so a reference resolves to the managed instance instead of a detached copy rebuilt from a DTO
    private final EntityManager entityManager;
    private static final int APPLY = 1;
    private static final int ROLLBACK = -1;

    @Override
    @Loggable(logArgs = false, logResult = false) // amounts, notes and balances are personal financial data
    public TransactionDto create(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        TransactionType transactionType = TransactionType.of(transactionCreateRequest.getTypeValue());
        // Validate before creating a payee: cheap checks first, no write ahead of a business error
        AccountDto sourceAccountDto = getAccountForTransaction(transactionCreateRequest.getSourceAccountId(), Set.of());
        AccountDto targetAccountDto = getAccountForTransaction(transactionCreateRequest.getTargetAccountId(), Set.of());
        validateTransaction(transactionType, sourceAccountDto, targetAccountDto);
        PayeeDto payeeDto = getOrCreatePayeeDto(transactionCreateRequest.getPayeeId(), transactionCreateRequest.getPayeeName(), transactionCreateRequest.getCategoryId(), null);
        applyBalanceEffect(transactionType, sourceAccountDto.getId(), idOf(targetAccountDto), transactionCreateRequest.getAmount(), APPLY);
        Transaction transaction = new Transaction();
        transaction.setUser(entityManager.getReference(WalletUser.class, userService.getCurrentUserId()));
        fill(transaction, transactionCreateRequest, transactionType, payeeDto, sourceAccountDto, targetAccountDto);
        return mapper.toTransactionDto(repository.save(transaction));
    }

    /**
     * Copies the request's fields and the resolved associations onto the transaction (shared by create and update).
     */
    private void fill(Transaction transaction, TransactionWriteRequest request, TransactionType type, PayeeDto payee,
                      AccountDto source, AccountDto target) {
        transaction.setType(type);
        transaction.setPayee(entityManager.getReference(Payee.class, payee.getId()));
        transaction.setAmount(request.getAmount());
        transaction.setDateTime(request.getDateTime());
        transaction.setNotes(request.getNotes());
        transaction.setSourceAccount(entityManager.getReference(Account.class, source.getId()));
        transaction.setTargetAccount(target != null ? entityManager.getReference(Account.class, target.getId()) : null);
    }

    /**
     * New transactions may only use ACTIVE payees. On update, the transaction's current payee is still allowed
     * even if it was deleted since, so old transactions stay editable.
     */
    private PayeeDto getOrCreatePayeeDto(Long payeeId, String payeeName, Long categoryId, Long existingPayeeId) throws MtgWalletGenericException {
        if (payeeId == TransactionWriteRequest.NEW_PAYEE_ID) {
            return payeeService.create(PayeeCreateRequest.builder().name(payeeName).categoryId(categoryId).build());
        }
        if (Objects.equals(payeeId, existingPayeeId)) {
            return payeeService.getPayee(payeeId);
        }
        return payeeService.getActivePayee(payeeId);
    }

    /**
     * Same rule as payees: ACTIVE accounts only, except the account the transaction already uses.
     */
    private AccountDto getAccountForTransaction(Long accountId, Set<Long> allowedInactiveIds) throws MtgWalletGenericException {
        if (accountId == null) {
            return null;
        }
        if (allowedInactiveIds.contains(accountId)) {
            return accountService.getAccountById(accountId);
        }
        return accountService.getActiveAccountById(accountId);
    }

    private static Long idOf(Account account) {
        return account != null ? account.getId() : null;
    }

    private static Long idOf(AccountDto account) {
        return account != null ? account.getId() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> search(TransactionSearchRequest request, Pageable pageable) {
        Specification<Transaction> specification = TransactionSpecification.search(request, userService.getCurrentUserId());
        Page<Transaction> transactions = repository.findAll(specification, pageable);
        return transactions.map(mapper::toTransactionDto);
    }

    @Override
    @Loggable(logArgs = false, safeArgs = 1, logResult = false) // keeps the transaction id only
    public TransactionDto update(TransactionUpdateRequest transactionUpdateRequest, Long id) throws MtgWalletGenericException {
        Transaction transaction = ownedTransaction(id);
        Long existingPayeeId = transaction.getPayee() != null ? transaction.getPayee().getId() : null;
        Set<Long> existingAccountIds = new HashSet<>();
        existingAccountIds.add(idOf(transaction.getSourceAccount()));
        existingAccountIds.add(idOf(transaction.getTargetAccount()));
        TransactionType transactionType = TransactionType.of(transactionUpdateRequest.getTypeValue());
        AccountDto sourceAccountDto = getAccountForTransaction(transactionUpdateRequest.getSourceAccountId(), existingAccountIds);
        AccountDto targetAccountDto = getAccountForTransaction(transactionUpdateRequest.getTargetAccountId(), existingAccountIds);
        validateTransaction(transactionType, sourceAccountDto, targetAccountDto);
        PayeeDto payeeDto = getOrCreatePayeeDto(transactionUpdateRequest.getPayeeId(), transactionUpdateRequest.getPayeeName(), transactionUpdateRequest.getCategoryId(), existingPayeeId);
        applyBalanceEffect(transaction.getType(), idOf(transaction.getSourceAccount()), idOf(transaction.getTargetAccount()), transaction.getAmount(), ROLLBACK);
        applyBalanceEffect(transactionType, sourceAccountDto.getId(), idOf(targetAccountDto), transactionUpdateRequest.getAmount(), APPLY);
        fill(transaction, transactionUpdateRequest, transactionType, payeeDto, sourceAccountDto, targetAccountDto);
        return mapper.toTransactionDto(repository.save(transaction));
    }

    @Override
    @Loggable
    public void delete(Long id) throws MtgWalletGenericException {
        Transaction transaction = ownedTransaction(id);
        applyBalanceEffect(transaction.getType(), idOf(transaction.getSourceAccount()), idOf(transaction.getTargetAccount()), transaction.getAmount(), ROLLBACK);
        repository.delete(transaction);
    }

    private Transaction ownedTransaction(Long id) throws MtgWalletGenericException {
        Transaction transaction = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.TRANSACTION_NOT_FOUND));
        userService.validateUserIdIfItsTheCurrentUser(transaction.getUser().getId());
        return transaction;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getProfitLossByCurrentUserAndDateIntervalAndCurrency(LocalDateTime startDate, LocalDateTime endDate, Currency currency) {
        if (startDate == null || endDate == null || currency == null) {
            throw new IllegalArgumentException("startDate, endDate and currency are required");
        }
        return repository.getProfitLossByUserIdAndDateIntervalAndCurrency(userService.getCurrentUserId(), startDate, endDate, currency, TransactionType.EXPENSE, List.of(TransactionType.EXPENSE, TransactionType.INCOME)).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionCreateScreenEnumResponse getTransactionCreateScreenEnums() {
        TransactionCreateScreenEnumResponse response = new TransactionCreateScreenEnumResponse();
        Page<CategoryResponse> categoriesPage = categoryService.search(CategorySearchRequest.builder().build(), Status.ACTIVE, allByName(MAX_CATEGORIES));
        Page<AccountResponse> accountsPage = accountService.search(AccountSearchRequest.builder().build(), Status.ACTIVE, allByName(MAX_ACCOUNTS));
        response.setCategoryList(categoriesPage.getContent());
        response.setAccountList(accountsPage.getContent());
        return response;
    }

    private void validateTransaction(TransactionType type, AccountDto source, AccountDto target) throws MtgWalletGenericException {
        if (source == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
        }
        if (type != TransactionType.TRANSFER) {
            if (target != null) {
                throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES);
            }
            return;
        }
        if (target == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
        }
        if (Objects.equals(source.getId(), target.getId())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.SOURCE_ACCOUNT_ID_CANT_BE_THE_SAME_AS_TARGET_ACCOUNT_ID);
        }
        if (target.getCurrency() != source.getCurrency()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_CURRENCY_SHOULD_BE_THE_SAME_AS_SOURCE_ACCOUNT_CURRENCY);
        }
    }

    /**
     * Applies (direction = APPLY) or reverts (direction = ROLLBACK) a transaction's effect on account balances.
     * Each change goes through AccountService.adjustBalance, so a second effect on the same account sees the first.
     */
    private void applyBalanceEffect(TransactionType type, Long sourceId, Long targetId, BigDecimal amount, int direction) throws MtgWalletGenericException {
        BigDecimal delta = amount.multiply(BigDecimal.valueOf(direction));
        switch (type) {
            case EXPENSE -> accountService.adjustBalance(sourceId, delta.negate());
            case INCOME -> accountService.adjustBalance(sourceId, delta);
            case TRANSFER -> {
                accountService.adjustBalance(sourceId, delta.negate());
                // validateTransaction guarantees a target on APPLY; legacy TRANSFER rows without one roll back the source only
                if (targetId != null) {
                    accountService.adjustBalance(targetId, delta);
                }
            }
        }
    }
}
