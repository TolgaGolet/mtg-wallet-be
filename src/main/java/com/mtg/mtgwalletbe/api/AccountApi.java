package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.response.AccountCreateResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountApi {
    private final AccountService accountService;
    private final AccountServiceMapper accountServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<AccountCreateResponse> create(@RequestBody @Validated AccountCreateRequest accountCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(accountServiceMapper.toAccountCreateResponse(accountService.create(accountCreateRequest)));
    }
}
