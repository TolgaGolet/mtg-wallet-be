package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.repository.ServiceLogRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// CQ-14: ServiceLog retention
class ServiceLogCleanupJobTest {

    @Test
    void purgeOldLogs_deletesRowsOlderThanRetention() {
        ServiceLogRepository repository = mock(ServiceLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T03:00:00Z"), ZoneOffset.UTC);

        new ServiceLogCleanupJob(repository, 180, clock).purgeOldLogs();

        verify(repository).deleteByCreatedDateBefore(LocalDateTime.of(2026, 5, 17, 3, 0));
    }
}
