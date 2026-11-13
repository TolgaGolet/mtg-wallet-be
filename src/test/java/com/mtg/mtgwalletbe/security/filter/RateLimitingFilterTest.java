package com.mtg.mtgwalletbe.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    private static final int MAX = 3;
    private final RateLimitingFilter filter = new RateLimitingFilter(MAX, 60);

    private int call(String remoteAddr, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void blocksAfterMaxRequestsPerIp() throws Exception {
        for (int i = 0; i < MAX; i++) {
            assertThat(call("1.1.1.1", null)).isEqualTo(200);
        }
        assertThat(call("1.1.1.1", null)).isEqualTo(429);
        assertThat(call("2.2.2.2", null)).isEqualTo(200);
    }

    // Regression for CQ-13: a client-supplied X-Forwarded-For must not create a fresh bucket
    @Test
    void spoofedForwardedForDoesNotBypassLimit() throws Exception {
        for (int i = 0; i < MAX; i++) {
            call("1.1.1.1", "10.0.0." + i);
        }
        assertThat(call("1.1.1.1", "9.9.9.9")).isEqualTo(429);
    }

    // Regression for CQ-13: counting is atomic, so parallel requests can't exceed the limit
    @Test
    void parallelRequestsAreCountedAtomically() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                calls.add(() -> call("3.3.3.3", null));
            }
            long passed = 0;
            for (Future<Integer> f : pool.invokeAll(calls)) {
                if (f.get() == 200) {
                    passed++;
                }
            }
            assertThat(passed).isEqualTo(MAX);
        } finally {
            pool.shutdownNow();
        }
    }
}
