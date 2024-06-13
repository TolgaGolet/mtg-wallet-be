package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.TransactionCreateRequest;
import com.mtg.mtgwalletbe.api.response.TransactionCreateResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.TransactionServiceMapper;
import com.mtg.mtgwalletbe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionApi {
    private final TransactionService transactionService;
    private final TransactionServiceMapper transactionServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<TransactionCreateResponse> create(@RequestBody @Validated TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(transactionServiceMapper.toTransactionCreateResponse(transactionService.create(transactionCreateRequest)));
    }
}
