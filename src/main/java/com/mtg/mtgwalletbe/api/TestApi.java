package com.mtg.mtgwalletbe.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"disabled-security", "dev", "test"})
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestApi {
    @GetMapping("/get-message")
    public ResponseEntity<String> getMessage() {
        return ResponseEntity.ok("success");
    }
}
