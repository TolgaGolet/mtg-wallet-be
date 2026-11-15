package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.repository.ServiceLogRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.HomeService;
import com.mtg.mtgwalletbe.service.HomeServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs @Loggable through the real Spring proxy (LoggableAspectTest mocks the join point, so it can't catch this).
 * Regression: binding "@annotation(loggable)" failed every call with "JoinPointMatch was NOT bound in invocation",
 * because the aspect runs ahead of ExposeInvocationInterceptor.
 */
@SpringBootTest
@ActiveProfiles("unittest")
class LoggableAspectContextTest {
    private static final String SERVICE_NAME = HomeServiceImpl.class.getName() + ".updateHomeCurrency";

    @Autowired private HomeService homeService;
    @Autowired private ServiceLogRepository serviceLogRepository;
    @Autowired private WalletUserRepository walletUserRepository;

    private WalletUser user;
    private Set<Long> logIdsBefore;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        // The context is shared with other tests: leave no rows behind
        serviceLogRepository.deleteAll(newLogs());
        if (user != null) {
            walletUserRepository.delete(user);
        }
    }

    @Test
    void loggableCallRunsThroughProxy_andWritesItsServiceLog() {
        user = walletUserRepository.save(WalletUser.builder().username("aspectuser").email("aspect@example.com")
                .name("aspect").password("hash").isDefaultsCreated(true).isEmailVerified(true).totpEnabled(false).build());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        logIdsBefore = serviceLogRepository.findAll().stream().map(ServiceLog::getId).collect(Collectors.toSet());

        homeService.updateHomeCurrency("EUR", "tab-1", 1L);

        assertThat(newLogs()).singleElement().satisfies(log -> {
            assertThat(log.getServiceName()).isEqualTo(SERVICE_NAME);
            assertThat(log.getStatus()).isEqualTo("S");
            assertThat(log.getRequest()).contains("EUR");
        });
        assertThat(walletUserRepository.findById(user.getId()).orElseThrow().getHomeCurrency()).isEqualTo(Currency.EUR);
    }

    private List<ServiceLog> newLogs() {
        return serviceLogRepository.findAll().stream()
                .filter(log -> logIdsBefore != null && !logIdsBefore.contains(log.getId()))
                .filter(log -> SERVICE_NAME.equals(log.getServiceName()))
                .toList();
    }
}
