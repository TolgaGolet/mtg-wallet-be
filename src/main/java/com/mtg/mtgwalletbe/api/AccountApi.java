package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AccountSaveRequest;
import com.mtg.mtgwalletbe.api.response.AccountSaveResponse;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountApi {
    private final AccountService accountService;
    private final AccountServiceMapper accountServiceMapper;

    @PostMapping("/save")
    public ResponseEntity<AccountSaveResponse> save(@RequestBody @Validated AccountSaveRequest accountSaveRequest) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/save").toUriString());
        return ResponseEntity.created(uri).body(accountServiceMapper.toAccountSaveResponse(accountService.save(accountSaveRequest)));
    }
}
