package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.api.request.PayeeUpdateRequest;
import com.mtg.mtgwalletbe.api.response.PayeeCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.PayeeResponse;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.service.PayeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.mtg.mtgwalletbe.security.SecurityParams.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/payee")
@RequiredArgsConstructor
public class PayeeApi {
    private final PayeeService payeeService;
    private final PayeeServiceMapper payeeServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<PayeeResponse> create(@RequestBody @Validated PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(payeeServiceMapper.toPayeeResponse(payeeService.create(payeeCreateRequest)));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<PayeeResponse>> search(@RequestBody @Validated PayeeSearchRequest request, @RequestParam(name = "pageNo", defaultValue = "0") int pageNo) {
        return ResponseEntity.ok(payeeService.search(request, Status.ACTIVE, PageRequest.of(pageNo, DEFAULT_PAGE_SIZE, Sort.by("name").ascending())));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<PayeeResponse> update(@RequestBody @Validated PayeeUpdateRequest payeeUpdateRequest, @PathVariable Long id) throws MtgWalletGenericException {
        return ResponseEntity.ok(payeeServiceMapper.toPayeeResponse(payeeService.update(payeeUpdateRequest, id)));
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws MtgWalletGenericException {
        payeeService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/create/enums")
    public ResponseEntity<PayeeCreateScreenEnumResponse> getCategoryCreateScreenEnums() {
        return ResponseEntity.ok(payeeService.getPayeeCreateScreenEnums());
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
