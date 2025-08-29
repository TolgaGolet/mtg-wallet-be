package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.api.request.AccountUpdateRequest;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.AccountRepository;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.specification.AccountSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {
    private final AccountRepository repository;
    private final AccountServiceMapper mapper;
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;
    public static final int MAX_ALLOWED_ACCOUNT_COUNT = 20;

    @Override
    public AccountDto create(AccountCreateRequest accountCreateRequest) throws MtgWalletGenericException {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        AccountType accountType = AccountType.of(accountCreateRequest.getTypeValue());
        Currency currency = Currency.of(accountCreateRequest.getCurrencyValue());
        if (accountCreateRequest.getBalance() == null) {
            accountCreateRequest.setBalance(BigDecimal.ZERO);
        }
        List<AccountDto> userAccounts = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userAccounts.size() >= MAX_ALLOWED_ACCOUNT_COUNT) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNTS_LIMIT_EXCEEDED.getMessage());
        }
        if (userAccounts.stream().anyMatch(account -> account.getName().equals(accountCreateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NAME_ALREADY_EXISTS.getMessage());
        }
        AccountDto accountDtoToSave = AccountDto.builder().userId(walletUserDto.getId()).name(accountCreateRequest.getName()).type(accountType)
                .balance(accountCreateRequest.getBalance()).currency(currency).status(Status.ACTIVE).build();
        return mapper.toAccountDto(repository.save(mapper.toAccountEntity(accountDtoToSave)));
    }

    @Override
    public Page<AccountResponse> search(AccountSearchRequest request, Status status, Pageable pageable) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        request.setUserId(walletUserDto.getId());
        Specification<Account> specification = AccountSpecification.search(request, status);
        Page<Account> accounts = repository.findAll(specification, pageable);
        return accounts.map(mapper::toAccountResponse);
    }

    @Override
    public List<AccountDto> findAllByCurrentUserByStatus(Status status) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        return mapper.toAccountDtoList(repository.findAllByUserAndStatus(userServiceMapper.toWalletUserEntity(walletUserDto), status));
    }

    @Override
    public AccountDto getAccountById(Long id) throws MtgWalletGenericException {
        Account account = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(account.getUser().getId());
        return mapper.toAccountDto(account);
    }

    @Override
    public AccountDto update(AccountUpdateRequest accountUpdateRequest, Long id) throws MtgWalletGenericException {
        Account account = repository.findByIdAndStatus(id, Status.ACTIVE).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(account.getUser().getId());
        List<AccountDto> userAccounts = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userAccounts.stream().anyMatch(existingAccount -> !Objects.equals(existingAccount.getId(), id) && existingAccount.getName().equals(accountUpdateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NAME_ALREADY_EXISTS.getMessage());
        }
        account.setName(accountUpdateRequest.getName());
        account.setType(AccountType.of(accountUpdateRequest.getTypeValue()));
        return mapper.toAccountDto(repository.save(account));
    }

    @Override
    public AccountDto update(AccountDto accountDto) throws MtgWalletGenericException {
        Account account = repository.findById(accountDto.getId()).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(account.getUser().getId());
        mapper.updateAccountFromDto(accountDto, account);
        return mapper.toAccountDto(repository.save(account));
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Account account = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(account.getUser().getId());
        account.setName("Deleted Account");
        account.setStatus(Status.DELETED);
        repository.save(account);
    }

    @Override
    public BigDecimal getTotalBalanceByCurrentUserAndCurrency(Currency currency) {
        List<AccountDto> userAccounts = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userAccounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return userAccounts.stream().filter(accountDto -> Objects.equals(accountDto.getCurrency(), currency)).map(AccountDto::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
