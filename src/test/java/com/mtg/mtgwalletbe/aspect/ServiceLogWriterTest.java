package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.repository.ServiceLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Regression: with @Transactional(REQUIRES_NEW), begin/commit failures happened outside the try and escaped
class ServiceLogWriterTest {

    private final ServiceLogRepository repository = mock(ServiceLogRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final ServiceLogWriter writer = new ServiceLogWriter(repository, transactionManager);
    private final ServiceLog serviceLog = ServiceLog.builder().serviceName("svc").status("E").build();

    @Test
    void savesInsideItsOwnTransaction() {
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(repository.save(serviceLog)).thenReturn(serviceLog);

        assertThat(writer.save(serviceLog)).isSameAs(serviceLog);
        verify(transactionManager).commit(any());
    }

    @Test
    void transactionCannotBeOpened_returnsNull() {
        when(transactionManager.getTransaction(any())).thenThrow(new CannotCreateTransactionException("pool exhausted"));

        assertThat(writer.save(serviceLog)).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void commitFails_returnsNull() {
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(repository.save(serviceLog)).thenReturn(serviceLog);
        doThrow(new TransactionSystemException("flush failed")).when(transactionManager).commit(any());

        assertThat(writer.save(serviceLog)).isNull();
    }
}
