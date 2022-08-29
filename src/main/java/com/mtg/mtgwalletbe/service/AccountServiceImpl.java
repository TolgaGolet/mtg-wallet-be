package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.repository.AccountRepository;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {
    private final AccountRepository repository;
    private final AccountServiceMapper mapper;
    private final UserService userService;

    @Override
    public AccountDto create(AccountCreateRequest accountCreateRequest) {
        WalletUserDto walletUserDto = userService.getUser(accountCreateRequest.getUsername());
        AccountType accountType = AccountType.of(accountCreateRequest.getTypeKey());
        Currency currency = Currency.of(accountCreateRequest.getCurrencyKey());
        if (accountCreateRequest.getBalance() == null) {
            accountCreateRequest.setBalance(BigDecimal.ZERO);
        }
        AccountDto accountDtoToSave = AccountDto.builder().user(walletUserDto).name(accountCreateRequest.getName()).type(accountType)
                .balance(accountCreateRequest.getBalance()).currency(currency).build();
        return mapper.toAccountDto(repository.save(mapper.toAccountEntity(accountDtoToSave)));
    }

    @Override
    public AccountDto getAccount(Long id) {
        Optional<Account> account = repository.findById(id);
        return account.map(mapper::toAccountDto).orElse(null);
    }

    @Override
    public AccountDto update(AccountDto accountDto) throws MtgWalletGenericException {
        Account account = repository.findById(accountDto.getId()).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND.getMessage()));
        mapper.updateAccountFromDto(accountDto, account);
        return mapper.toAccountDto(repository.save(account));
    }
}
