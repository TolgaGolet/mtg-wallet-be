package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountSaveRequest;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.repository.AccountRepository;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {
    private final AccountRepository repository;
    private final AccountServiceMapper mapper;
    private final UserService userService;

    @Override
    public AccountDto save(AccountSaveRequest accountSaveRequest) {
        WalletUserDto walletUserDto = userService.getUser(accountSaveRequest.getUsername());
        AccountType accountType = AccountType.of(accountSaveRequest.getTypeKey());
        Currency currency = Currency.of(accountSaveRequest.getCurrencyKey());
        if (accountSaveRequest.getBalance() == null) {
            accountSaveRequest.setBalance(BigDecimal.ZERO);
        }
        AccountDto accountDtoToSave = AccountDto.builder().user(walletUserDto).name(accountSaveRequest.getName()).type(accountType)
                .balance(accountSaveRequest.getBalance()).currency(currency).build();
        return mapper.toAccountDto(repository.save(mapper.toAccountEntity(accountDtoToSave)));
    }
}
