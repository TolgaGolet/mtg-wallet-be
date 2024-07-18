package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.response.GetNetValueResponse;
import com.mtg.mtgwalletbe.api.response.HomeScreenEnumResponse;
import com.mtg.mtgwalletbe.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeApi {
    private final HomeService homeService;

    @GetMapping("/enums")
    public ResponseEntity<HomeScreenEnumResponse> getHomeScreenEnums() {
        return ResponseEntity.ok(new HomeScreenEnumResponse());
    }

    @GetMapping("/net-value")
    public ResponseEntity<GetNetValueResponse> getNetValue(@RequestParam(name = "currencyValue") String currencyValue, @RequestParam(name = "intervalValue") String intervalValue) {
        return ResponseEntity.ok(homeService.getNetValue(currencyValue, intervalValue));
    }
}
