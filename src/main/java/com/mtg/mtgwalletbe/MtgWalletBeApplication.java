package com.mtg.mtgwalletbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MtgWalletBeApplication {
    public static void main(String[] args) {
        SpringApplication.run(MtgWalletBeApplication.class, args);
    }
}
