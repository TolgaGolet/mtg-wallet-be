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
import com.mtg.mtgwalletbe.repository.AccountRepository;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.specification.AccountSpecification;
import com.mtg.mtgwalletbe.util.Names;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_ACCOUNTS;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {
    private final AccountRepository repository;
    private final AccountServiceMapper mapper;
    private final UserService userService;

    @Override
    public AccountDto create(AccountCreateRequest accountCreateRequest) throws MtgWalletGenericException {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        AccountType accountType = AccountType.of(accountCreateRequest.getTypeValue());
        Currency currency = Currency.of(accountCreateRequest.getCurrencyValue());
        if (accountCreateRequest.getBalance() == null) {
            accountCreateRequest.setBalance(BigDecimal.ZERO);
        }
        if (repository.countByUserIdAndStatus(walletUserDto.getId(), Status.ACTIVE) >= MAX_ACCOUNTS) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNTS_LIMIT_EXCEEDED);
        }
        if (nameTaken(walletUserDto.getId(), accountCreateRequest.getName(), -1L)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NAME_ALREADY_EXISTS);
        }
        AccountDto accountDtoToSave = AccountDto.builder().userId(walletUserDto.getId()).name(accountCreateRequest.getName()).type(accountType)
                .balance(accountCreateRequest.getBalance()).currency(currency).status(Status.ACTIVE).build();
        return mapper.toAccountDto(repository.save(mapper.toAccountEntity(accountDtoToSave)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> search(AccountSearchRequest request, Status status, Pageable pageable) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        request.setUserId(walletUserDto.getId());
        Specification<Account> specification = AccountSpecification.search(request, status);
        Page<Account> accounts = repository.findAll(specification, pageable);
        return accounts.map(mapper::toAccountResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDto> findAllByCurrentUserByStatus(Status status) {
        return mapper.toAccountDtoList(repository.findAllByUserIdAndStatus(userService.getCurrentUserId(), status));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccountById(Long id) throws MtgWalletGenericException {
        return mapper.toAccountDto(owned(repository.findById(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getActiveAccountById(Long id) throws MtgWalletGenericException {
        return mapper.toAccountDto(owned(repository.findByIdAndStatus(id, Status.ACTIVE)));
    }

    @Override
    public AccountDto update(AccountUpdateRequest accountUpdateRequest, Long id) throws MtgWalletGenericException {
        Account account = owned(repository.findByIdAndStatus(id, Status.ACTIVE));
        if (nameTaken(account.getUser().getId(), accountUpdateRequest.getName(), id)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NAME_ALREADY_EXISTS);
        }
        account.setName(accountUpdateRequest.getName());
        account.setType(AccountType.of(accountUpdateRequest.getTypeValue()));
        return mapper.toAccountDto(repository.save(account));
    }

    @Override
    public AccountDto update(AccountDto accountDto) throws MtgWalletGenericException {
        Account account = owned(repository.findById(accountDto.getId()));
        mapper.updateAccountFromDto(accountDto, account);
        return mapper.toAccountDto(repository.save(account));
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Account account = owned(repository.findById(id));
        account.setName("Deleted Account");
        account.setStatus(Status.DELETED);
        repository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceByCurrentUserAndCurrency(Currency currency) {
        List<AccountDto> userAccounts = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userAccounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return userAccounts.stream().filter(accountDto -> Objects.equals(accountDto.getCurrency(), currency)).map(AccountDto::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Case-insensitive (Turkish rules) name check against the user's other ACTIVE accounts; loads names only.
     */
    private boolean nameTaken(Long userId, String name, Long excludeId) {
        return Names.containsName(repository.findNamesByUserIdAndStatus(userId, Status.ACTIVE, excludeId), name);
    }

    /**
     * Unwraps a lookup: ACCOUNT_NOT_FOUND when empty, and the account must belong to the current user.
     */
    private Account owned(Optional<Account> lookup) throws MtgWalletGenericException {
        Account account = lookup.orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND));
        userService.validateUserIdIfItsTheCurrentUser(account.getUser().getId());
        return account;
    }
}
