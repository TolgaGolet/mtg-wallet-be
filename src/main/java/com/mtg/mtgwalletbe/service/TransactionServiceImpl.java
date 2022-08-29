package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.TransactionCreateRequest;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.TransactionServiceMapper;
import com.mtg.mtgwalletbe.repository.TransactionRepository;
import com.mtg.mtgwalletbe.service.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository repository;
    private final TransactionServiceMapper mapper;
    private final PayeeService payeeService;
    private final AccountService accountService;
    private final UserService userService;

    // TODO owner checks(authorization) by getcurrentloggedinuser on all services. ex: account creation, check user username
    @Override
    public TransactionDto create(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        WalletUserDto walletUserDto = userService.getUser(transactionCreateRequest.getUsername());
        TransactionType transactionType = TransactionType.of(transactionCreateRequest.getTypeKey());
        PayeeDto payeeDto = payeeService.getPayee(transactionCreateRequest.getPayeeId());
        AccountDto sourceAccountDto = accountService.getAccount(transactionCreateRequest.getSourceAccountId());
        AccountDto targetAccountDto = transactionCreateRequest.getTargetAccountId() != null ? accountService.getAccount(transactionCreateRequest.getTargetAccountId()) : null;
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
                .user(walletUserDto).build();
        return mapper.toTransactionDto(repository.save(mapper.toTransactionEntity(transactionDtoToSave)));
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
