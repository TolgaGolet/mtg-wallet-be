package com.mtg.mtgwalletbe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    private HomeServiceImpl homeService;

    @BeforeEach
    void setUp() {
        homeService = new HomeServiceImpl(accountService, transactionService);
        when(accountService.getTotalBalanceByCurrentUserAndCurrency(any()))
                .thenReturn(BigDecimal.ONE);
        when(transactionService.getProfitLossByCurrentUserAndDateIntervalAndCurrency(
                any(), any(), any()))
                .thenReturn(BigDecimal.ONE);
    }

    @Test
    void getStartDateByInterval_ForDailyInterval_ReturnsCorrectDate() {
        // Act
        homeService.getNetValue("USD", "D");

        // Assert
        LocalDateTime now = LocalDateTime.now();
        verify(transactionService).getProfitLossByCurrentUserAndDateIntervalAndCurrency(
                argThat(date -> date.isBefore(now) && date.isAfter(now.minusDays(2))),
                any(),
                any()
        );
    }

    @Test
    void getStartDateByInterval_ForWeeklyInterval_ReturnsCorrectDate() {
        // Act
        homeService.getNetValue("USD", "W");

        // Assert
        LocalDateTime now = LocalDateTime.now();
        verify(transactionService).getProfitLossByCurrentUserAndDateIntervalAndCurrency(
                argThat(date -> date.isBefore(now) && date.isAfter(now.minusWeeks(2))),
                any(),
                any()
        );
    }

    @Test
    void getStartDateByInterval_ForMonthlyInterval_ReturnsCorrectDate() {
        // Act
        homeService.getNetValue("USD", "M");

        // Assert
        LocalDateTime now = LocalDateTime.now();
        verify(transactionService).getProfitLossByCurrentUserAndDateIntervalAndCurrency(
                argThat(date -> date.isBefore(now) && date.isAfter(now.minusMonths(2))),
                any(),
                any()
        );
    }

    @Test
    void getStartDateByInterval_ForYearlyInterval_ReturnsCorrectDate() {
        // Act
        homeService.getNetValue("USD", "Y");

        // Assert
        LocalDateTime now = LocalDateTime.now();
        verify(transactionService).getProfitLossByCurrentUserAndDateIntervalAndCurrency(
                argThat(date -> date.isBefore(now) && date.isAfter(now.minusYears(2))),
                any(),
                any()
        );
    }
}