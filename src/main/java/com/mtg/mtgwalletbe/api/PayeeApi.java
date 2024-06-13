package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.api.response.PayeeCreateResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.service.PayeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payee")
@RequiredArgsConstructor
public class PayeeApi {
    private final PayeeService payeeService;
    private final PayeeServiceMapper payeeServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<PayeeCreateResponse> create(@RequestBody @Validated PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(payeeServiceMapper.toPayeeCreateResponse(payeeService.create(payeeCreateRequest)));
    }

    @PostMapping("/add-default-payee-for-expense-to-user")
    public ResponseEntity<Void> addDefaultPayeeForExpenseToUser(@RequestParam(name = "payeeId") Long payeeId) throws MtgWalletGenericException {
        payeeService.addDefaultPayeeForExpenseToUser(payeeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/add-default-payee-for-income-to-user")
    public ResponseEntity<Void> addDefaultPayeeForIncomeToUser(@RequestParam(name = "payeeId") Long payeeId) throws MtgWalletGenericException {
        payeeService.addDefaultPayeeForIncomeToUser(payeeId);
        return ResponseEntity.ok().build();
    }
}
