package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.TransactionCreateRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.TransactionDto;

public interface TransactionService {
    TransactionDto create(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException;
}
