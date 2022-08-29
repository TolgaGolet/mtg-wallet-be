package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.AccountDto;

public interface AccountService {
    AccountDto create(AccountCreateRequest accountDto);

    public AccountDto getAccount(Long id);

    AccountDto update(AccountDto accountDto) throws MtgWalletGenericException;
}
