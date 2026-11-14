package com.mtg.mtgwalletbe.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static com.mtg.mtgwalletbe.security.SecurityParams.ERROR_CODE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {
    // The frontend reads X-Error-Code cross-origin; without this it gets null in every profile
    @Test
    void exposesErrorCodeHeader() {
        CorsConfiguration configuration = new CorsConfig().corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/transaction/search"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getExposedHeaders()).contains(ERROR_CODE_HEADER);
    }
}
