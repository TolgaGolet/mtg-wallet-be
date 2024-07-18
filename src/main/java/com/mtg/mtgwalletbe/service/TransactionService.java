package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.TransactionCreateRequest;
import com.mtg.mtgwalletbe.api.request.TransactionSearchRequest;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.TransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionService {
    TransactionDto create(TransactionCreateRequest transactionCreateRequest) throws MtgWalletGenericException;

    Page<TransactionDto> search(TransactionSearchRequest request, Pageable pageable);

    BigDecimal getProfitLossByCurrentUserAndDateIntervalAndCurrency(LocalDateTime startDate, LocalDateTime endDate, Currency currency);
}
