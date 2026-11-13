package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.annotation.Loggable;
import com.mtg.mtgwalletbe.api.request.*;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.TransactionCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Transaction;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.mapper.TransactionServiceMapper;
import com.mtg.mtgwalletbe.repository.TransactionRepository;
import com.mtg.mtgwalletbe.service.dto.*;
import com.mtg.mtgwalletbe.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository repository;
    private final TransactionServiceMapper mapper;
    private final AccountServiceMapper accountServiceMapper;
    private final PayeeServiceMapper payeeServiceMapper;
    private final PayeeService payeeService;
    private final AccountService accountService;
    private final UserService userService;
    private final CategoryService categoryService;
    private static final int APPLY = 1;
    private static final int ROLLBACK = -1;

    @Override
    @Loggable
    public TransactionDto create(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        TransactionType transactionType = TransactionType.of(transactionCreateRequest.getTypeValue());
        PayeeDto payeeDto = getOrCreatePayeeDto(transactionCreateRequest.getPayeeId(), transactionCreateRequest.getPayeeName(), transactionCreateRequest.getCategoryId(), null);
        AccountDto sourceAccountDto = getAccountForTransaction(transactionCreateRequest.getSourceAccountId(), Set.of());
        AccountDto targetAccountDto = getAccountForTransaction(transactionCreateRequest.getTargetAccountId(), Set.of());
        validateTransaction(ValidateTransactionDto.builder()
                .sourceAccountDto(sourceAccountDto)
                .targetAccountDto(targetAccountDto)
                .transactionType(transactionType).build());
        applyBalanceEffect(transactionType, sourceAccountDto, targetAccountDto, transactionCreateRequest.getAmount(), APPLY);
        TransactionDto transactionDtoToSave = TransactionDto.builder().type(transactionType)
                .payee(payeeDto)
                .amount(transactionCreateRequest.getAmount())
                .dateTime(transactionCreateRequest.getDateTime())
                .sourceAccount(sourceAccountDto)
                .sourceAccountNewBalance(sourceAccountDto.getBalance())
                .targetAccountNewBalance(targetAccountDto != null ? targetAccountDto.getBalance() : null)
                .targetAccount(targetAccountDto)
                .notes(transactionCreateRequest.getNotes())
                .userId(walletUserDto.getId()).build();
        return mapper.toTransactionDto(repository.save(mapper.toTransactionEntity(transactionDtoToSave)));
    }

    /**
     * New transactions may only use ACTIVE payees. On update, the transaction's current payee is still allowed
     * even if it was deleted since, so old transactions stay editable.
     */
    private PayeeDto getOrCreatePayeeDto(Long payeeId, String payeeName, Long categoryId, Long existingPayeeId) throws MtgWalletGenericException {
        if (payeeId == TransactionCreateRequest.NEW_PAYEE_ID) {
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

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> search(TransactionSearchRequest request, Pageable pageable) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        request.setUserId(walletUserDto.getId());
        Specification<Transaction> specification = TransactionSpecification.search(request);
        Page<Transaction> transactions = repository.findAll(specification, pageable);
        return transactions.map(mapper::toTransactionDto);
    }

    @Override
    @Loggable
    public TransactionDto update(TransactionUpdateRequest transactionUpdateRequest, Long id) throws MtgWalletGenericException {
        Transaction transaction = ownedTransaction(id);
        Long existingPayeeId = transaction.getPayee() != null ? transaction.getPayee().getId() : null;
        Set<Long> existingAccountIds = new HashSet<>();
        existingAccountIds.add(idOf(transaction.getSourceAccount()));
        existingAccountIds.add(idOf(transaction.getTargetAccount()));
        PayeeDto payeeDto = getOrCreatePayeeDto(transactionUpdateRequest.getPayeeId(), transactionUpdateRequest.getPayeeName(), transactionUpdateRequest.getCategoryId(), existingPayeeId);
        AccountDto sourceAccountDto = getAccountForTransaction(transactionUpdateRequest.getSourceAccountId(), existingAccountIds);
        AccountDto targetAccountDto = getAccountForTransaction(transactionUpdateRequest.getTargetAccountId(), existingAccountIds);
        TransactionType transactionType = TransactionType.of(transactionUpdateRequest.getTypeValue());
        validateTransaction(ValidateTransactionDto.builder()
                .sourceAccountDto(sourceAccountDto)
                .targetAccountDto(targetAccountDto)
                .transactionType(transactionType).build());
        applyBalanceEffect(transaction.getType(), accountServiceMapper.toAccountDto(transaction.getSourceAccount()), accountServiceMapper.toAccountDto(transaction.getTargetAccount()), transaction.getAmount(), ROLLBACK);
        // Re-read the validated accounts once: the rollback may have changed their balances (persistence-context hit, no query)
        AccountDto updatedSourceAccountDto = accountService.getAccountById(sourceAccountDto.getId());
        AccountDto updatedTargetAccountDto = targetAccountDto != null ? accountService.getAccountById(targetAccountDto.getId()) : null;
        applyBalanceEffect(transactionType, updatedSourceAccountDto, updatedTargetAccountDto, transactionUpdateRequest.getAmount(), APPLY);
        transaction.setPayee(payeeServiceMapper.toPayeeEntity(payeeDto));
        transaction.setType(transactionType);
        transaction.setAmount(transactionUpdateRequest.getAmount());
        transaction.setDateTime(transactionUpdateRequest.getDateTime());
        transaction.setNotes(transactionUpdateRequest.getNotes());
        transaction.setSourceAccountNewBalance(updatedSourceAccountDto.getBalance());
        transaction.setTargetAccountNewBalance(updatedTargetAccountDto != null ? updatedTargetAccountDto.getBalance() : null);
        transaction.setSourceAccount(accountServiceMapper.toAccountEntity(updatedSourceAccountDto));
        transaction.setTargetAccount(accountServiceMapper.toAccountEntity(updatedTargetAccountDto));
        return mapper.toTransactionDto(repository.save(transaction));
    }

    @Override
    @Loggable
    public void delete(Long id) throws MtgWalletGenericException {
        Transaction transaction = ownedTransaction(id);
        applyBalanceEffect(transaction.getType(), accountServiceMapper.toAccountDto(transaction.getSourceAccount()), accountServiceMapper.toAccountDto(transaction.getTargetAccount()), transaction.getAmount(), ROLLBACK);
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
            throw new IllegalArgumentException();
        }
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        return repository.getProfitLossByUserIdAndDateIntervalAndCurrency(walletUserDto.getId(), startDate, endDate, currency, TransactionType.EXPENSE, List.of(TransactionType.EXPENSE, TransactionType.INCOME)).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionCreateScreenEnumResponse getTransactionCreateScreenEnums() {
        TransactionCreateScreenEnumResponse response = new TransactionCreateScreenEnumResponse();
        Page<CategoryResponse> categoriesPage = categoryService.search(CategorySearchRequest.builder().build(), Status.ACTIVE, PageRequest.of(0, MAX_CATEGORIES, Sort.by("name").ascending()));
        Page<AccountResponse> accountsPage = accountService.search(AccountSearchRequest.builder().build(), Status.ACTIVE, PageRequest.of(0, MAX_ACCOUNTS, Sort.by("name").ascending()));
        response.setCategoryList(categoriesPage.getContent());
        response.setAccountList(accountsPage.getContent());
        return response;
    }

    private void validateTransaction(ValidateTransactionDto validateTransactionDto) throws MtgWalletGenericException {
        if (validateTransactionDto.getSourceAccountDto() == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && validateTransactionDto.getTargetAccountDto() == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
        }
        if (validateTransactionDto.getTransactionType() != TransactionType.TRANSFER
                && validateTransactionDto.getTargetAccountDto() != null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES);
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && Objects.equals(validateTransactionDto.getSourceAccountDto().getId(), validateTransactionDto.getTargetAccountDto().getId())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.SOURCE_ACCOUNT_ID_CANT_BE_THE_SAME_AS_TARGET_ACCOUNT_ID);
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && validateTransactionDto.getTargetAccountDto().getCurrency() != validateTransactionDto.getSourceAccountDto().getCurrency()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_CURRENCY_SHOULD_BE_THE_SAME_AS_SOURCE_ACCOUNT_CURRENCY);
        }
    }

    /**
     * Applies (direction = APPLY) or reverts (direction = ROLLBACK) a transaction's effect on account balances.
     */
    private void applyBalanceEffect(TransactionType type, AccountDto source, AccountDto target, BigDecimal amount, int direction) throws MtgWalletGenericException {
        BigDecimal delta = amount.multiply(BigDecimal.valueOf(direction));
        switch (type) {
            case EXPENSE -> adjustBalance(source, delta.negate());
            case INCOME -> adjustBalance(source, delta);
            case TRANSFER -> {
                adjustBalance(source, delta.negate());
                // validateTransaction guarantees a target on APPLY; legacy TRANSFER rows without one roll back the source only
                if (target != null) {
                    adjustBalance(target, delta);
                }
            }
        }
    }

    private void adjustBalance(AccountDto account, BigDecimal delta) throws MtgWalletGenericException {
        account.setBalance(account.getBalance().add(delta));
        accountService.update(account);
    }
}
