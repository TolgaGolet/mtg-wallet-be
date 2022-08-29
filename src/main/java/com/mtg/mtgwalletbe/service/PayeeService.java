package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;

public interface PayeeService {
    PayeeDto create(PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException;

    public PayeeDto getPayee(Long id);

    void addDefaultPayeeForExpenseToUser(String username, Long payeeId) throws MtgWalletGenericException;

    void addDefaultPayeeForIncomeToUser(String username, Long payeeId) throws MtgWalletGenericException;
}
