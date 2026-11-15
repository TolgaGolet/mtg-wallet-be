package com.mtg.mtgwalletbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class MtgWalletBeApplication {
    // The JVM default timezone is set by config.TimeZoneInitializer (registered in META-INF/spring.factories)
    public static void main(String[] args) {
        SpringApplication.run(MtgWalletBeApplication.class, args);
    }
}
