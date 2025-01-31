package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.response.GetNetValueResponse;

public interface HomeService {
    GetNetValueResponse getNetValue(String currencyValue, String intervalValue);
}
