package com.mtg.mtgwalletbe.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetNetValueResponse {
    private BigDecimal netValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercentage;
}
