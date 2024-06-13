package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.AccountRepository;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {
    private final AccountRepository repository;
    private final AccountServiceMapper mapper;
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;

    @Override
    public AccountDto create(AccountCreateRequest accountCreateRequest) throws MtgWalletGenericException {
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        AccountType accountType = AccountType.of(accountCreateRequest.getTypeKey());
        Currency currency = Currency.of(accountCreateRequest.getCurrencyKey());
        if (accountCreateRequest.getBalance() == null) {
            accountCreateRequest.setBalance(BigDecimal.ZERO);
        }
        List<AccountDto> userAccounts = findAllByCurrentUser();
        if (userAccounts.stream().anyMatch(account -> account.getName().equals(accountCreateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NAME_ALREADY_EXISTS.getMessage());
        }
        AccountDto accountDtoToSave = AccountDto.builder().user(walletUserDto).name(accountCreateRequest.getName()).type(accountType)
                .balance(accountCreateRequest.getBalance()).currency(currency).build();
        return mapper.toAccountDto(repository.save(mapper.toAccountEntity(accountDtoToSave)));
    }

    @Override
    public List<AccountDto> findAllByCurrentUser() {
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        return mapper.toAccountDtoList(repository.findAllByUser(userServiceMapper.toWalletUserEntity(walletUserDto)));
    }

    @Override
    public AccountDto getAccountById(Long id) throws MtgWalletGenericException {
        Account account = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()));
        userService.validateUsernameIfItsTheCurrentUser(account.getUser().getUsername());
        return mapper.toAccountDto(account);
    }

    @Override
    public AccountDto update(AccountDto accountDto) throws MtgWalletGenericException {
        Account account = repository.findById(accountDto.getId()).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()));
        userService.validateUsernameIfItsTheCurrentUser(account.getUser().getUsername());
        mapper.updateAccountFromDto(accountDto, account);
        return mapper.toAccountDto(repository.save(account));
    }
}
