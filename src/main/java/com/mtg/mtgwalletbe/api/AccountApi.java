package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.response.AccountCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountApi {
    private final AccountService accountService;
    private final AccountServiceMapper accountServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> create(@RequestBody @Validated AccountCreateRequest accountCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(accountServiceMapper.toAccountResponse(accountService.create(accountCreateRequest)));
    }

    @GetMapping("/get-users-all")
    public ResponseEntity<List<AccountResponse>> getUsersAll() {
        return ResponseEntity.ok(accountServiceMapper.toAccountResponseList(accountService.findAllByCurrentUser()));
    }

    @GetMapping("/create/enums")
    public ResponseEntity<AccountCreateScreenEnumResponse> getAccountCreateScreenEnums() {
        return ResponseEntity.ok(new AccountCreateScreenEnumResponse());
    }
}
