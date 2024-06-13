package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;

import java.util.List;

public interface PayeeService {
    PayeeDto create(PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException;

    public List<PayeeDto> findAllByCurrentUser();

    public PayeeDto getPayee(Long id) throws MtgWalletGenericException;

    void addDefaultPayeeForExpenseToUser(Long payeeId) throws MtgWalletGenericException;

    void addDefaultPayeeForIncomeToUser(Long payeeId) throws MtgWalletGenericException;
}
