package com.mtg.mtgwalletbe.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds mtgWallet.frontend.* (FRONTEND_URL env var via the profile properties files). Used for links in emails.
 */
@Validated
@ConfigurationProperties(prefix = "mtg-wallet.frontend")
public record FrontendProperties(@NotBlank String url) {
}
