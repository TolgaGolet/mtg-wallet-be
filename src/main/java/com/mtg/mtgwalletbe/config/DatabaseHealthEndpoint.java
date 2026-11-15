package com.mtg.mtgwalletbe.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * /actuator/database-health: answers DOWN with 503 (never throws) when no valid connection can be obtained. A custom
 * endpoint gets no status mapping, unlike /actuator/health, so the status code is set here for monitors that read only it.
 */
@Component
@RequiredArgsConstructor
@WebEndpoint(id = "database-health")
@Slf4j
public class DatabaseHealthEndpoint {
    private static final int VALIDATION_TIMEOUT_SECONDS = 3;
    private final DataSource dataSource;

    @ReadOperation
    public WebEndpointResponse<Health> health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                return new WebEndpointResponse<>(Health.up().withDetail("database", "Database is up").build());
            }
            log.warn("Database health check failed: connection is not valid");
        } catch (SQLException e) {
            log.warn("Database health check failed: {}", e.getClass().getSimpleName());
        }
        return new WebEndpointResponse<>(Health.down().withDetail("database", "Database is down").build(),
                WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
    }
}
