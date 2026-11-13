package com.mtg.mtgwalletbe.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds mtgWallet.applicationConfig.* (e.g. DEFAULT_PAGE_SIZE env var via the profile properties files).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mtg-wallet.application-config")
public class ApplicationConfigProperties {
    private int defaultPageSize = 10;
}
