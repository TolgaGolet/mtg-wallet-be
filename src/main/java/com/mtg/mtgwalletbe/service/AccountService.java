package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccountService {
    AccountDto create(AccountCreateRequest accountDto) throws MtgWalletGenericException;

    Page<AccountResponse> search(AccountSearchRequest request, Pageable pageable);

    public List<AccountDto> findAllByCurrentUser();

    public AccountDto getAccountById(Long id) throws MtgWalletGenericException;

    AccountDto update(AccountDto accountDto) throws MtgWalletGenericException;
}
