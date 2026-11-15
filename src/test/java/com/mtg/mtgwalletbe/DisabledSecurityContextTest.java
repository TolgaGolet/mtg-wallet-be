package com.mtg.mtgwalletbe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Regression for F1: the documented disabled-security profile failed at startup (beans needing profile-gated ones)
@SpringBootTest
@ActiveProfiles({"unittest", "disabled-security"})
class DisabledSecurityContextTest {

    @Test
    void contextLoads() {
    }
}
