package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.response.AccountCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.AccountDetailsResponse;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.service.AccountService;
import com.mtg.mtgwalletbe.service.TransactionService;
import com.mtg.mtgwalletbe.service.dto.TransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.mtg.mtgwalletbe.security.SecurityParams.DEFAULT_PAGE_SIZE;

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

    @GetMapping("/get-users-all")
    public ResponseEntity<List<AccountResponse>> getUsersAll() {
        return ResponseEntity.ok(accountServiceMapper.toAccountResponseList(accountService.findAllByCurrentUser()));
    }

    @GetMapping("/create/enums")
    public ResponseEntity<AccountCreateScreenEnumResponse> getAccountCreateScreenEnums() {
        return ResponseEntity.ok(new AccountCreateScreenEnumResponse());
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<AccountDetailsResponse> getAccountDetails(@PathVariable(name = "id") Long id, @RequestParam(name = "pageNo") int pageNo) throws MtgWalletGenericException {
        AccountDetailsResponse accountDetailsResponse = accountService.getAccountDetails(id, pageNo);
        Page<TransactionDto> transactions = transactionService.findUserTransactionsByAccount(accountServiceMapper.toAccountEntity(accountDetailsResponse), PageRequest.of(pageNo, DEFAULT_PAGE_SIZE, Sort.by("dateTime").descending()));
        accountDetailsResponse.setTransactions(transactions);
        return ResponseEntity.ok(accountDetailsResponse);
    }
}
