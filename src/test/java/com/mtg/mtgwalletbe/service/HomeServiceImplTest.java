package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.response.GetNetValueResponse;
import com.mtg.mtgwalletbe.enums.Currency;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// CQ-05: profit/loss percentage edge cases (patched by hand in e30f519)
@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock private AccountService accountService;
    @Mock private TransactionService transactionService;
    @InjectMocks private HomeServiceImpl service;

    @ParameterizedTest(name = "net={0}, P/L={1} -> {2}%")
    @CsvSource({
            "1000, 0, 0",        // no activity
            "1100, 100, 10.00",  // initial 1000, +100
            "900, -100, -10.00", // initial 1000, -100
            "100, 100, 100",     // initial balance 0 -> capped at +100%
            "-50, 100, 100",     // initial balance negative -> sign only
            "-150, -100, -100"   // initial -50, loss -> -100%
    })
    void profitLossPercentage(String netValue, String profitLoss, String expectedPercentage) {
        when(accountService.getTotalBalanceByCurrentUserAndCurrency(Currency.TRY)).thenReturn(new BigDecimal(netValue));
        when(transactionService.getProfitLossByCurrentUserAndDateIntervalAndCurrency(any(), any(), eq(Currency.TRY))).thenReturn(new BigDecimal(profitLoss));

        GetNetValueResponse response = service.getNetValue("TRY", "M");

        assertThat(response.getNetValue()).isEqualByComparingTo(netValue);
        assertThat(response.getProfitLoss()).isEqualByComparingTo(profitLoss);
        assertThat(response.getProfitLossPercentage()).isEqualByComparingTo(expectedPercentage);
    }
}
