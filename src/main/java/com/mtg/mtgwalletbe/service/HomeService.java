package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.response.GetNetValueResponse;
import com.mtg.mtgwalletbe.api.response.HomeScreenEnumResponse;

public interface HomeService {
    GetNetValueResponse getNetValue(String currencyValue, String intervalValue);

    HomeScreenEnumResponse getHomeScreenEnums();

    /** `clientId` and `sequence` are optional; see {@link UserService#setHomeCurrency}. */
    void updateHomeCurrency(String currencyValue, String clientId, Long sequence);
}
