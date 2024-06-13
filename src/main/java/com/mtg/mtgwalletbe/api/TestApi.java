package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.annotation.Loggable;
import com.mtg.mtgwalletbe.service.UserService;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"disabled-security", "test"})
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestApi {
    private final UserService userService;

    @Loggable
    @GetMapping("/get-message")
    public ResponseEntity<String> getMessage() {
        return ResponseEntity.ok("success");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/get-user/{username}")
    public ResponseEntity<WalletUserDto> getUser(@PathVariable(name = "username") String username) {
        return ResponseEntity.ok(userService.getUser(username));
    }
}
