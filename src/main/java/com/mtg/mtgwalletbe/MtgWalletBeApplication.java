package com.mtg.mtgwalletbe;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableAspectJAutoProxy
public class MtgWalletBeApplication {
    @Value("${mtgWallet.applicationConfig.timeZone}")
    private String timeZone;

    public static void main(String[] args) {
        SpringApplication.run(MtgWalletBeApplication.class, args);
    }

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
    }
}
