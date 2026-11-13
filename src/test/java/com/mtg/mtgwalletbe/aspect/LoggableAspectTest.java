package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.annotation.Loggable;
import com.mtg.mtgwalletbe.entity.ServiceLog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoggableAspectTest {

    private static final String SECRET_LINK = "https://app/reset-password?token=secret-token";

    private ServiceLogWriter repository;
    private LoggableAspect aspect;
    private ProceedingJoinPoint joinPoint;

    // Annotated samples to obtain real Loggable instances
    @Loggable
    void defaultLoggable() {
    }

    @Loggable(logArgs = false, logResult = false)
    void redactedLoggable() {
    }

    // Same setup as MailServiceImpl.send(to, subject, body)
    @Loggable(logArgs = false, safeArgs = 1, emailArgs = 0)
    void mailLoggable() {
    }

    private static Loggable loggableOf(String methodName) throws NoSuchMethodException {
        return LoggableAspectTest.class.getDeclaredMethod(methodName).getAnnotation(Loggable.class);
    }

    @BeforeEach
    void setUp() {
        repository = mock(ServiceLogWriter.class);
        aspect = new LoggableAspect(repository);
        joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("send");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"user@example.com", "Subject", SECRET_LINK});
    }

    private ServiceLog savedLog() {
        ArgumentCaptor<ServiceLog> captor = ArgumentCaptor.forClass(ServiceLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void defaultLoggable_storesArgsAndResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result-value");

        Object result = aspect.logMethodCall(joinPoint, loggableOf("defaultLoggable"));

        assertThat(result).isEqualTo("result-value");
        ServiceLog log = savedLog();
        assertThat(log.getRequest()).contains("user@example.com", SECRET_LINK);
        assertThat(log.getResponse()).isEqualTo("result-value");
        assertThat(log.getStatus()).isEqualTo("S");
    }

    // Regression for CQ-02: mail args (recipient + body with token links) must not be persisted
    @Test
    void logArgsFalse_redactsRequestAndResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result-with-secret");

        Object result = aspect.logMethodCall(joinPoint, loggableOf("redactedLoggable"));

        assertThat(result).isEqualTo("result-with-secret");
        ServiceLog log = savedLog();
        assertThat(log.getRequest()).isEqualTo("[[redacted], [redacted], [redacted]]");
        assertThat(log.getResponse()).isEqualTo(LoggableAspect.REDACTED);
        assertThat(log.getServiceName()).endsWith(".send");
    }

    @Test
    void mailLoggable_keepsMaskedRecipientAndSubject_redactsBody() throws Throwable {
        when(joinPoint.proceed()).thenReturn(null);

        aspect.logMethodCall(joinPoint, loggableOf("mailLoggable"));

        ServiceLog log = savedLog();
        assertThat(log.getRequest()).isEqualTo("[u***@example.com, Subject, [redacted]]");
        assertThat(log.getRequest()).doesNotContain("user@example.com", "secret-token");
    }

    @Test
    void logArgsFalse_onException_storesOnlyExceptionClass() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("failed for " + SECRET_LINK));

        assertThatThrownBy(() -> aspect.logMethodCall(joinPoint, loggableOf("redactedLoggable")))
                .isInstanceOf(IllegalStateException.class);

        ServiceLog log = savedLog();
        assertThat(log.getStatus()).isEqualTo("E");
        assertThat(log.getRequest()).doesNotContain("secret-token");
        assertThat(log.getResponse()).isEqualTo(IllegalStateException.class.getName());
    }
}
