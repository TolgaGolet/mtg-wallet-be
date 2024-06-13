package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.AccountDto;

import java.util.List;

public interface AccountService {
    AccountDto create(AccountCreateRequest accountDto) throws MtgWalletGenericException;

    public List<AccountDto> findAllByCurrentUser();

    public AccountDto getAccountById(Long id) throws MtgWalletGenericException;

    AccountDto update(AccountDto accountDto) throws MtgWalletGenericException;
}
