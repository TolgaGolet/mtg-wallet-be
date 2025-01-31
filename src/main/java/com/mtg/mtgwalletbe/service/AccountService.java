package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.api.request.AccountUpdateRequest;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountDto create(AccountCreateRequest accountDto) throws MtgWalletGenericException;

    Page<AccountResponse> search(AccountSearchRequest request, Status status, Pageable pageable);

    public List<AccountDto> findAllByCurrentUserByStatus(Status status);

    public AccountDto getAccountById(Long id) throws MtgWalletGenericException;

    AccountDto update(AccountUpdateRequest accountUpdateRequest, Long id) throws MtgWalletGenericException;

    AccountDto update(AccountDto accountDto) throws MtgWalletGenericException;

    void delete(Long id) throws MtgWalletGenericException;

    BigDecimal getTotalBalanceByCurrentUserAndCurrency(Currency currency);
}
