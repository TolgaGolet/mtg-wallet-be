package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.repository.ServiceLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Keeps the ServiceLog table bounded: deletes rows older than mtgWallet.serviceLog.retentionDays (default 180).
 */
@Slf4j
@Component
public class ServiceLogCleanupJob {
    private final ServiceLogRepository serviceLogRepository;
    private final int retentionDays;
    private final Clock clock;

    @Autowired
    public ServiceLogCleanupJob(ServiceLogRepository serviceLogRepository,
                                @Value("${mtgWallet.serviceLog.retentionDays:180}") int retentionDays) {
        this(serviceLogRepository, retentionDays, Clock.systemDefaultZone());
    }

    ServiceLogCleanupJob(ServiceLogRepository serviceLogRepository, int retentionDays, Clock clock) {
        this.serviceLogRepository = serviceLogRepository;
        this.retentionDays = retentionDays;
        this.clock = clock;
    }

    @Scheduled(cron = "${mtgWallet.serviceLog.cleanupCron:0 0 3 * * *}")
    @Transactional
    public void purgeOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        int deleted = serviceLogRepository.deleteByCreatedDateBefore(cutoff);
        log.info("ServiceLog cleanup: deleted {} rows older than {} days", deleted, retentionDays);
    }
}
