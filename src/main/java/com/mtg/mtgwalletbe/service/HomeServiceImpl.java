package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.response.GetNetValueResponse;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Interval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class HomeServiceImpl implements HomeService {
    private final AccountService accountService;
    private final TransactionService transactionService;

    @Override
    public GetNetValueResponse getNetValue(String currencyValue, String intervalValue) {
        Currency currency = Currency.of(currencyValue);
        Interval interval = Interval.of(intervalValue);
        GetNetValueResponse response = new GetNetValueResponse();
        response.setNetValue(accountService.getTotalBalanceByCurrentUserAndCurrency(currency));
        response.setProfitLoss(transactionService.getProfitLossByCurrentUserAndDateIntervalAndCurrency(getStartDateByInterval(interval), LocalDateTime.now(), currency));
        if (response.getProfitLoss().compareTo(BigDecimal.ZERO) == 0) {
            response.setProfitLossPercentage(BigDecimal.ZERO);
        } else if (response.getNetValue().compareTo(BigDecimal.ZERO) == 0) {
            response.setProfitLossPercentage(BigDecimal.ZERO);
        } else {
            response.setProfitLossPercentage(response.getProfitLoss().multiply(BigDecimal.valueOf(100)).divide(response.getNetValue(), RoundingMode.HALF_UP));
        }
        return response;
    }

    private LocalDateTime getStartDateByInterval(Interval interval) {
        LocalDateTime startDate = LocalDateTime.now();
        startDate = switch (interval) {
            case Interval.DAILY -> startDate.minusDays(1);
            case Interval.WEEKLY -> startDate.minusWeeks(1);
            case Interval.MONTHLY -> startDate.minusMonths(1);
            case Interval.YEARLY -> startDate.minusYears(1);
        };
        return startDate;
    }
}
