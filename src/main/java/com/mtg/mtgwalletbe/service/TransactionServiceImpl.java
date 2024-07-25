package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.annotation.Loggable;
import com.mtg.mtgwalletbe.api.request.*;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.TransactionCreateScreenEnumResponse;
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
import java.util.List;
import java.util.Objects;

import static com.mtg.mtgwalletbe.service.AccountServiceImpl.MAX_ALLOWED_ACCOUNT_COUNT;
import static com.mtg.mtgwalletbe.service.CategoryServiceImpl.MAX_ALLOWED_CATEGORY_COUNT;

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

    @Override
    @Loggable
    public TransactionDto create(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        TransactionType transactionType = TransactionType.of(transactionCreateRequest.getTypeValue());
        PayeeDto payeeDto = getOrCreatePayeeDto(transactionCreateRequest.getPayeeId(), transactionCreateRequest.getPayeeName(), transactionCreateRequest.getCategoryId());
        AccountDto sourceAccountDto = accountService.getAccountById(transactionCreateRequest.getSourceAccountId());
        AccountDto targetAccountDto = transactionCreateRequest.getTargetAccountId() != null ? accountService.getAccountById(transactionCreateRequest.getTargetAccountId()) : null;
        validateTransaction(ValidateTransactionDto.builder()
                .payeeDto(payeeDto)
                .sourceAccountDto(sourceAccountDto)
                .targetAccountDto(targetAccountDto)
                .transactionType(transactionType).build());
        updateAccountBalances(UpdateAccountBalancesDto.builder()
                .sourceAccountDto(sourceAccountDto)
                .targetAccountDto(targetAccountDto)
                .amount(transactionCreateRequest.getAmount())
                .transactionType(transactionType).build());
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

    private PayeeDto getOrCreatePayeeDto(Long payeeId, String payeeName, Long categoryId) throws MtgWalletGenericException {
        if (payeeId == -1L) {
            return payeeService.create(PayeeCreateRequest.builder().name(payeeName).categoryId(categoryId).build());
        }
        return payeeService.getPayee(payeeId);
    }

    @Override
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
        Transaction transaction = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.TRANSACTION_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(transaction.getUser().getId());
        PayeeDto payeeDto = getOrCreatePayeeDto(transactionUpdateRequest.getPayeeId(), transactionUpdateRequest.getPayeeName(), transactionUpdateRequest.getCategoryId());
        AccountDto sourceAccountDto = accountService.getAccountById(transactionUpdateRequest.getSourceAccountId());
        AccountDto targetAccountDto = transactionUpdateRequest.getTargetAccountId() != null ? accountService.getAccountById(transactionUpdateRequest.getTargetAccountId()) : null;
        TransactionType transactionType = TransactionType.of(transactionUpdateRequest.getTypeValue());
        validateTransaction(ValidateTransactionDto.builder()
                .payeeDto(payeeDto)
                .sourceAccountDto(sourceAccountDto)
                .targetAccountDto(targetAccountDto)
                .transactionType(transactionType).build());
        rollbackAccountBalances(RollbackAccountBalancesDto.builder()
                .transactionType(transaction.getType())
                .sourceAccountDto(accountServiceMapper.toAccountDto(transaction.getSourceAccount()))
                .targetAccountDto(accountServiceMapper.toAccountDto(transaction.getTargetAccount()))
                .amount(transaction.getAmount())
                .build());
        AccountDto updatedSourceAccountDto = accountService.getAccountById(transactionUpdateRequest.getSourceAccountId());
        AccountDto updatedTargetAccountDto = transactionUpdateRequest.getTargetAccountId() != null ? accountService.getAccountById(transactionUpdateRequest.getTargetAccountId()) : null;
        updateAccountBalances(UpdateAccountBalancesDto.builder()
                .sourceAccountDto(updatedSourceAccountDto)
                .targetAccountDto(updatedTargetAccountDto)
                .amount(transactionUpdateRequest.getAmount())
                .transactionType(transactionType).build());
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
        Transaction transaction = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.TRANSACTION_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(transaction.getUser().getId());
        rollbackAccountBalances(RollbackAccountBalancesDto.builder()
                .transactionType(transaction.getType())
                .sourceAccountDto(accountServiceMapper.toAccountDto(transaction.getSourceAccount()))
                .targetAccountDto(accountServiceMapper.toAccountDto(transaction.getTargetAccount()))
                .amount(transaction.getAmount())
                .build());
        repository.delete(transaction);
    }

    @Override
    public BigDecimal getProfitLossByCurrentUserAndDateIntervalAndCurrency(LocalDateTime startDate, LocalDateTime endDate, Currency currency) {
        if (startDate == null || endDate == null || currency == null) {
            throw new IllegalArgumentException();
        }
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        return repository.getProfitLossByUserIdAndDateIntervalAndCurrency(walletUserDto.getId(), startDate, endDate, currency, TransactionType.EXPENSE, List.of(TransactionType.EXPENSE, TransactionType.INCOME)).orElse(BigDecimal.ZERO);
    }

    @Override
    public TransactionCreateScreenEnumResponse getTransactionCreateScreenEnums() {
        TransactionCreateScreenEnumResponse response = new TransactionCreateScreenEnumResponse();
        Page<CategoryResponse> categoriesPage = categoryService.search(CategorySearchRequest.builder().build(), Status.ACTIVE, PageRequest.of(0, MAX_ALLOWED_CATEGORY_COUNT, Sort.by("name").ascending()));
        Page<AccountResponse> accountsPage = accountService.search(AccountSearchRequest.builder().build(), Status.ACTIVE, PageRequest.of(0, MAX_ALLOWED_ACCOUNT_COUNT, Sort.by("name").ascending()));
        response.setCategoryList(categoriesPage);
        response.setAccountList(accountsPage);
        return response;
    }

    private void validateTransaction(ValidateTransactionDto validateTransactionDto) throws MtgWalletGenericException {
        if (validateTransactionDto.getPayeeDto() == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage());
        }
        if (validateTransactionDto.getSourceAccountDto() == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage());
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && validateTransactionDto.getTargetAccountDto() == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage());
        }
        if (validateTransactionDto.getTransactionType() != TransactionType.TRANSFER
                && validateTransactionDto.getTargetAccountDto() != null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES.getMessage());
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && Objects.equals(validateTransactionDto.getSourceAccountDto().getId(), validateTransactionDto.getTargetAccountDto().getId())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.SOURCE_ACCOUNT_ID_CANT_BE_THE_SAME_AS_TARGET_ACCOUNT_ID.getMessage());
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && validateTransactionDto.getTargetAccountDto().getCurrency() != validateTransactionDto.getSourceAccountDto().getCurrency()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_CURRENCY_SHOULD_BE_THE_SAME_AS_SOURCE_ACCOUNT_CURRENCY.getMessage());
        }
    }

    private void updateAccountBalances(UpdateAccountBalancesDto updateAccountBalancesDto) throws MtgWalletGenericException {
        if (updateAccountBalancesDto.getTransactionType() == TransactionType.EXPENSE) {
            updateAccountBalancesDto.getSourceAccountDto()
                    .setBalance(updateAccountBalancesDto.getSourceAccountDto().getBalance()
                            .subtract(updateAccountBalancesDto.getAmount()));
            accountService.update(updateAccountBalancesDto.getSourceAccountDto());
        } else if (updateAccountBalancesDto.getTransactionType() == TransactionType.INCOME) {
            updateAccountBalancesDto.getSourceAccountDto()
                    .setBalance(updateAccountBalancesDto.getSourceAccountDto().getBalance()
                            .add(updateAccountBalancesDto.getAmount()));
            accountService.update(updateAccountBalancesDto.getSourceAccountDto());
        } else if (updateAccountBalancesDto.getTransactionType() == TransactionType.TRANSFER
                && updateAccountBalancesDto.getTargetAccountDto() != null) {
            updateAccountBalancesDto.getSourceAccountDto()
                    .setBalance(updateAccountBalancesDto.getSourceAccountDto().getBalance()
                            .subtract(updateAccountBalancesDto.getAmount()));
            accountService.update(updateAccountBalancesDto.getSourceAccountDto());
            updateAccountBalancesDto.getTargetAccountDto()
                    .setBalance(updateAccountBalancesDto.getTargetAccountDto().getBalance()
                            .add(updateAccountBalancesDto.getAmount()));
            accountService.update(updateAccountBalancesDto.getTargetAccountDto());
        }
    }

    private void rollbackAccountBalances(RollbackAccountBalancesDto rollbackAccountBalancesDto) throws MtgWalletGenericException {
        if (rollbackAccountBalancesDto.getTransactionType() == TransactionType.EXPENSE) {
            rollbackAccountBalancesDto.getSourceAccountDto()
                    .setBalance(rollbackAccountBalancesDto.getSourceAccountDto().getBalance()
                            .add(rollbackAccountBalancesDto.getAmount()));
            accountService.update(rollbackAccountBalancesDto.getSourceAccountDto());
        } else if (rollbackAccountBalancesDto.getTransactionType() == TransactionType.INCOME) {
            rollbackAccountBalancesDto.getSourceAccountDto()
                    .setBalance(rollbackAccountBalancesDto.getSourceAccountDto().getBalance()
                            .subtract(rollbackAccountBalancesDto.getAmount()));
            accountService.update(rollbackAccountBalancesDto.getSourceAccountDto());
        } else if (rollbackAccountBalancesDto.getTransactionType() == TransactionType.TRANSFER
                && rollbackAccountBalancesDto.getTargetAccountDto() != null) {
            rollbackAccountBalancesDto.getSourceAccountDto()
                    .setBalance(rollbackAccountBalancesDto.getSourceAccountDto().getBalance()
                            .add(rollbackAccountBalancesDto.getAmount()));
            accountService.update(rollbackAccountBalancesDto.getSourceAccountDto());
            rollbackAccountBalancesDto.getTargetAccountDto()
                    .setBalance(rollbackAccountBalancesDto.getTargetAccountDto().getBalance()
                            .subtract(rollbackAccountBalancesDto.getAmount()));
            accountService.update(rollbackAccountBalancesDto.getTargetAccountDto());
        }
    }
}
