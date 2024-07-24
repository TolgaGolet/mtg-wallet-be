package com.mtg.mtgwalletbe.service;

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
    private final PayeeService payeeService;
    private final AccountService accountService;
    private final UserService userService;
    private final CategoryService categoryService;

    @Override
    public TransactionDto create(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        TransactionType transactionType = TransactionType.of(transactionCreateRequest.getTypeValue());
        PayeeDto payeeDto = getOrCreatePayeeDto(transactionCreateRequest);
        AccountDto sourceAccountDto = accountService.getAccountById(transactionCreateRequest.getSourceAccountId());
        AccountDto targetAccountDto = transactionCreateRequest.getTargetAccountId() != null ? accountService.getAccountById(transactionCreateRequest.getTargetAccountId()) : null;
        validateTransaction(ValidateTransactionDto.builder()
                .payeeDto(payeeDto)
                .sourceAccountDto(sourceAccountDto)
                .targetAccountDto(targetAccountDto)
                .transactionCreateRequest(transactionCreateRequest)
                .transactionType(transactionType).build());
        updateAccountBalances(UpdateAccountBalancesDto.builder()
                .sourceAccountDto(sourceAccountDto)
                .targetAccountDto(targetAccountDto)
                .transactionCreateRequest(transactionCreateRequest)
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

    private PayeeDto getOrCreatePayeeDto(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        if (transactionCreateRequest.getPayeeId() == -1L) {
            return payeeService.create(PayeeCreateRequest.builder().name(transactionCreateRequest.getPayeeName()).categoryId(transactionCreateRequest.getCategoryId()).build());
        }
        return payeeService.getPayee(transactionCreateRequest.getPayeeId());
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
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()
                    + " with id: " + validateTransactionDto.getTransactionCreateRequest().getSourceAccountId());
        }
        if (validateTransactionDto.getTransactionCreateRequest().getTargetAccountId() != null
                && validateTransactionDto.getTargetAccountDto() == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage() + " with id: "
                    + validateTransactionDto.getTransactionCreateRequest().getTargetAccountId());
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && validateTransactionDto.getTransactionCreateRequest().getTargetAccountId() == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_ID_CANT_BE_EMPTY_FOR_TRANSFERS.getMessage());
        }
        if (validateTransactionDto.getTransactionType() != TransactionType.TRANSFER
                && validateTransactionDto.getTransactionCreateRequest().getTargetAccountId() != null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.TARGET_ACCOUNT_ID_SHOULD_BE_EMPTY_FOR_EXPENSES_AND_INCOMES.getMessage());
        }
        if (validateTransactionDto.getTransactionType() == TransactionType.TRANSFER
                && Objects.equals(validateTransactionDto.getTransactionCreateRequest().getSourceAccountId(), validateTransactionDto.getTransactionCreateRequest().getTargetAccountId())) {
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
                            .subtract(updateAccountBalancesDto.getTransactionCreateRequest().getAmount()));
            accountService.update(updateAccountBalancesDto.getSourceAccountDto());
        } else if (updateAccountBalancesDto.getTransactionType() == TransactionType.INCOME) {
            updateAccountBalancesDto.getSourceAccountDto()
                    .setBalance(updateAccountBalancesDto.getSourceAccountDto().getBalance()
                            .add(updateAccountBalancesDto.getTransactionCreateRequest().getAmount()));
            accountService.update(updateAccountBalancesDto.getSourceAccountDto());
        } else if (updateAccountBalancesDto.getTransactionType() == TransactionType.TRANSFER
                && updateAccountBalancesDto.getTargetAccountDto() != null) {
            updateAccountBalancesDto.getSourceAccountDto()
                    .setBalance(updateAccountBalancesDto.getSourceAccountDto().getBalance()
                            .subtract(updateAccountBalancesDto.getTransactionCreateRequest().getAmount()));
            accountService.update(updateAccountBalancesDto.getSourceAccountDto());
            updateAccountBalancesDto.getTargetAccountDto()
                    .setBalance(updateAccountBalancesDto.getTargetAccountDto().getBalance()
                            .add(updateAccountBalancesDto.getTransactionCreateRequest().getAmount()));
            accountService.update(updateAccountBalancesDto.getTargetAccountDto());
        }
    }
}
