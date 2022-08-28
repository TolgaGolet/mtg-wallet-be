package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountSaveRequest;
import com.mtg.mtgwalletbe.service.dto.AccountDto;

public interface AccountService {
    AccountDto save(AccountSaveRequest accountDto);
}
