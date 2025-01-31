package com.mtg.mtgwalletbe.security.api;

import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
@Endpoint(id = "database-health")
@Slf4j
public class DatabaseHealthActuator {
    private final DataSource dataSource;

    @ReadOperation
    public Health health() throws MtgWalletGenericException {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(3)) {
                return Health.up().withDetail("database", "Database is up").build();
            } else {
                log.error("Database health check failed");
                throw new MtgWalletGenericException(GenericExceptionMessages.DATABASE_DOWN.getMessage());
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            throw new MtgWalletGenericException(GenericExceptionMessages.DATABASE_DOWN.getMessage());
        }
    }
}
