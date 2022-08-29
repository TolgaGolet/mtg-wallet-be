package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.api.response.PayeeCreateResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.service.PayeeService;
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
@RequestMapping("/payee")
@RequiredArgsConstructor
public class PayeeApi {
    private final PayeeService payeeService;
    private final PayeeServiceMapper payeeServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<PayeeCreateResponse> create(@RequestBody @Validated PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/payee/create").toUriString());
        return ResponseEntity.created(uri).body(payeeServiceMapper.toPayeeCreateResponse(payeeService.create(payeeCreateRequest)));
    }
}
