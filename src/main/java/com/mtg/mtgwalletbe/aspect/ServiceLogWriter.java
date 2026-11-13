package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.repository.ServiceLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Persists ServiceLog rows in their own transaction, so a rollback of the business operation doesn't drop the
 * log row, and never throws, so a DB outage can't hide the original error or break the response.
 * The transaction is opened and committed inside the try (TransactionTemplate, not @Transactional), so failures
 * to begin or commit it are caught too.
 */
@Slf4j
@Component
public class ServiceLogWriter {
    private final ServiceLogRepository serviceLogRepository;
    private final TransactionTemplate requiresNew;

    public ServiceLogWriter(ServiceLogRepository serviceLogRepository, PlatformTransactionManager transactionManager) {
        this.serviceLogRepository = serviceLogRepository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * @return the saved row, or null when it couldn't be saved (the failure is logged to the application log)
     */
    public ServiceLog save(ServiceLog serviceLog) {
        try {
            return requiresNew.execute(status -> serviceLogRepository.save(serviceLog));
        } catch (RuntimeException e) {
            log.error("Could not persist ServiceLog for {} (status {})", serviceLog.getServiceName(), serviceLog.getStatus(), e);
            return null;
        }
    }
}
