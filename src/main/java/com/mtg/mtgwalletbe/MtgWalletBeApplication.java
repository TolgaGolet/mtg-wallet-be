package com.mtg.mtgwalletbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.RollbackOn;

@SpringBootApplication
@EnableJpaAuditing
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
// MtgWalletGenericException is checked: without this, a business error thrown after a write would commit that write
@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)
public class MtgWalletBeApplication {
    // The JVM default timezone is set by config.TimeZoneInitializer (registered in META-INF/spring.factories)
    public static void main(String[] args) {
        SpringApplication.run(MtgWalletBeApplication.class, args);
    }
}
