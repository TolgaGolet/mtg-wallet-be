package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.api.request.PayeeUpdateRequest;
import com.mtg.mtgwalletbe.api.response.PayeeCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.PayeeResponse;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PayeeService {
    PayeeDto create(PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException;

    void createDefaults() throws MtgWalletGenericException;

    Page<PayeeResponse> search(PayeeSearchRequest request, Status status, Pageable pageable);

    PayeeDto update(PayeeUpdateRequest payeeUpdateRequest, Long id) throws MtgWalletGenericException;

    void delete(Long id) throws MtgWalletGenericException;

    public List<PayeeDto> findAllByCurrentUserByStatus(Status status);

    public PayeeDto getPayee(Long id) throws MtgWalletGenericException;

    PayeeCreateScreenEnumResponse getPayeeCreateScreenEnums();

    void addDefaultPayeeForExpenseToUser(Long payeeId) throws MtgWalletGenericException;

    void addDefaultPayeeForIncomeToUser(Long payeeId) throws MtgWalletGenericException;
}
