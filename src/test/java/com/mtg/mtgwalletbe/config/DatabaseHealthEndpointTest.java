package com.mtg.mtgwalletbe.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The endpoint used to throw a checked business exception (500) instead of answering DOWN; then DOWN answered 200
class DatabaseHealthEndpointTest {
    private final DataSource dataSource = mock(DataSource.class);
    private final DatabaseHealthEndpoint endpoint = new DatabaseHealthEndpoint(dataSource);

    @Test
    void validConnection_isUp() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(dataSource.getConnection()).thenReturn(connection);

        WebEndpointResponse<Health> response = endpoint.health();

        assertThat(response.getBody().getStatus()).isEqualTo(Status.UP);
        assertThat(response.getStatus()).isEqualTo(WebEndpointResponse.STATUS_OK);
    }

    @Test
    void invalidConnection_isDown() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.isValid(anyInt())).thenReturn(false);
        when(dataSource.getConnection()).thenReturn(connection);

        WebEndpointResponse<Health> response = endpoint.health();

        assertThat(response.getBody().getStatus()).isEqualTo(Status.DOWN);
        assertThat(response.getStatus()).isEqualTo(WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
    }

    @Test
    void connectionFailure_isDown_withoutThrowing() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("refused"));

        WebEndpointResponse<Health> response = endpoint.health();
        Health health = response.getBody();

        assertThat(response.getStatus()).isEqualTo(WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "Database is down");
    }
}
