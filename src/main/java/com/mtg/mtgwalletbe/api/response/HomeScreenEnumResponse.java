package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeScreenEnumResponse {
    private Set<Currency> currencies = Set.of(Currency.values());
}
