package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.config.ApplicationConfigProperties;
import com.mtg.mtgwalletbe.api.request.TransactionCreateRequest;
import com.mtg.mtgwalletbe.api.request.TransactionSearchRequest;
import com.mtg.mtgwalletbe.api.request.TransactionUpdateRequest;
import com.mtg.mtgwalletbe.api.response.TransactionCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.TransactionListResponse;
import com.mtg.mtgwalletbe.api.response.TransactionResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.TransactionServiceMapper;
import com.mtg.mtgwalletbe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionApi {
    private final ApplicationConfigProperties applicationConfigProperties;
    private final TransactionService transactionService;
    private final TransactionServiceMapper transactionServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<TransactionResponse> create(@RequestBody @Validated TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(transactionServiceMapper.toTransactionResponse(transactionService.create(transactionCreateRequest)));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<TransactionListResponse>> search(@RequestBody @Validated TransactionSearchRequest request, @RequestParam(name = "pageNo", defaultValue = "0") int pageNo) {
        return ResponseEntity.ok(transactionService.search(request, PageRequest.of(pageNo, applicationConfigProperties.getDefaultPageSize(), Sort.by("dateTime").descending())).map(transactionServiceMapper::toTransactionListResponse));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<TransactionResponse> update(@RequestBody @Validated TransactionUpdateRequest transactionUpdateRequest, @PathVariable Long id) throws MtgWalletGenericException {
        return ResponseEntity.ok(transactionServiceMapper.toTransactionResponse(transactionService.update(transactionUpdateRequest, id)));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws MtgWalletGenericException {
        transactionService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/create/enums")
    public ResponseEntity<TransactionCreateScreenEnumResponse> getTransactionCreateScreenEnums() {
        return ResponseEntity.ok(transactionService.getTransactionCreateScreenEnums());
    }
}
