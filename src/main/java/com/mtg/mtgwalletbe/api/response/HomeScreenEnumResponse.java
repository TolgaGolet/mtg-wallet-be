package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeScreenEnumResponse {
    private List<Currency> currencies; // declared order: the frontend falls back to the first one
    private Currency selectedCurrency; // null until the user picks one
}
