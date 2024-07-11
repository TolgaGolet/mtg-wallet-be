package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.api.response.AccountCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.service.AccountService;
import com.mtg.mtgwalletbe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.mtg.mtgwalletbe.service.AccountServiceImpl.MAX_ALLOWED_ACCOUNT_COUNT;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountApi {
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AccountServiceMapper accountServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> create(@RequestBody @Validated AccountCreateRequest accountCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(accountServiceMapper.toAccountResponse(accountService.create(accountCreateRequest)));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<AccountResponse>> search(@RequestBody @Validated AccountSearchRequest request, @RequestParam(name = "pageNo", defaultValue = "0") int pageNo) {
        return ResponseEntity.ok(accountService.search(request, PageRequest.of(pageNo, MAX_ALLOWED_ACCOUNT_COUNT)));
    }

    @GetMapping("/create/enums")
    public ResponseEntity<AccountCreateScreenEnumResponse> getAccountCreateScreenEnums() {
        return ResponseEntity.ok(new AccountCreateScreenEnumResponse());
    }
}
