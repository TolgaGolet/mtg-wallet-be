package com.mtg.mtgwalletbe.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.util.TimeZone;

/**
 * Sets the JVM default timezone from mtgWallet.applicationConfig.timeZone as soon as the environment is ready,
 * before any bean (datasource, Hibernate, Jackson) is created and could read the old default. Registered in
 * META-INF/spring.factories, so it also runs for @SpringBootTest.
 */
public class TimeZoneInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    static final String PROPERTY = "mtgWallet.applicationConfig.timeZone";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        String zone = event.getEnvironment().getProperty(PROPERTY);
        if (zone != null && !zone.isBlank()) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
        }
    }
}
