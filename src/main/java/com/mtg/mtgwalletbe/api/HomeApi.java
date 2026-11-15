package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.UpdateHomeCurrencyRequest;
import com.mtg.mtgwalletbe.api.response.GetNetValueResponse;
import com.mtg.mtgwalletbe.api.response.HomeScreenEnumResponse;
import com.mtg.mtgwalletbe.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeApi {
    private final HomeService homeService;

    @GetMapping("/enums")
    public ResponseEntity<HomeScreenEnumResponse> getHomeScreenEnums() {
        return ResponseEntity.ok(homeService.getHomeScreenEnums());
    }

    @GetMapping("/net-value")
    public ResponseEntity<GetNetValueResponse> getNetValue(@RequestParam(name = "currencyValue") String currencyValue, @RequestParam(name = "intervalValue") String intervalValue) {
        return ResponseEntity.ok(homeService.getNetValue(currencyValue, intervalValue));
    }

    @PostMapping("/currency")
    public ResponseEntity<Void> updateHomeCurrency(@RequestBody @Validated UpdateHomeCurrencyRequest request) {
        homeService.updateHomeCurrency(request.getCurrencyValue(), request.getClientId(), request.getSequence());
        return ResponseEntity.noContent().build();
    }
}
