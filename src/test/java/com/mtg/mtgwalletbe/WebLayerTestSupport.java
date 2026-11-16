package com.mtg.mtgwalletbe;

import com.mtg.mtgwalletbe.aspect.ServiceLogWriter;
import com.mtg.mtgwalletbe.exception.MtgWalletExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;

/**
 * Web-layer tests without a Spring context: the real controller and exception
 * handler, bean validation on, no security filters. Valid sample values satisfy the request classes' patterns.
 */
public final class WebLayerTestSupport {
    public static final String VALID_PASSWORD = "Passw0rd!";
    public static final String VALID_USERNAME = "alice";
    public static final String VALID_EMAIL = "alice@example.com";

    private WebLayerTestSupport() {
    }

    public static MockMvc mvcFor(Object controller) {
        return mvcFor(controller, mock(ServiceLogWriter.class));
    }

    /** For tests that verify what the exception handler writes to the ServiceLog. */
    public static MockMvc mvcFor(Object controller, ServiceLogWriter serviceLogWriter) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MtgWalletExceptionHandler(serviceLogWriter))
                .build();
    }

    /**
     * A JSON request body, written with single quotes for readability: {@code post(url).with(json("{'a':'b'}"))}.
     * Every single quote becomes a double quote, so a body containing an apostrophe needs {@link #rawJson}.
     */
    public static RequestPostProcessor json(String singleQuotedJson) {
        return rawJson(singleQuotedJson.replace('\'', '"'));
    }

    /** A JSON request body sent exactly as given. */
    public static RequestPostProcessor rawJson(String body) {
        return request -> {
            request.setContentType(MediaType.APPLICATION_JSON_VALUE);
            request.setContent(body.getBytes(StandardCharsets.UTF_8));
            return request;
        };
    }
}
