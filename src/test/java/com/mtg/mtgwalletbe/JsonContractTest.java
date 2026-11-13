package com.mtg.mtgwalletbe;

import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.TransactionCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.TransactionListResponse;
import com.mtg.mtgwalletbe.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the JSON shapes the frontend depends on, through the same message converters Spring MVC uses
 * (guards the Boot 4 / Jackson 3 upgrade): Spring page fields, enums as {value,label} objects, ISO date-times.
 */
@SpringBootTest
@ActiveProfiles("unittest")
class JsonContractTest {

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    // Regression: Jackson 3 can't create @Builder-only classes (no default constructor), which broke the accounts page
    @Test
    void everyRequestBodyType_canBeReadFromJson() throws Exception {
        Set<Class<?>> bodyTypes = new TreeSet<>(Comparator.comparing(Class::getName));
        for (HandlerMethod method : handlerMapping.getHandlerMethods().values()) {
            for (MethodParameter parameter : method.getMethodParameters()) {
                if (parameter.hasParameterAnnotation(RequestBody.class)) {
                    bodyTypes.add(parameter.getParameterType());
                }
            }
        }
        assertThat(bodyTypes).hasSizeGreaterThan(20);

        List<String> failures = new ArrayList<>();
        for (Class<?> type : bodyTypes) {
            try {
                readJson("{}", type);
            } catch (Exception e) {
                failures.add(type.getSimpleName() + ": " + e.getMessage());
            }
        }
        assertThat(failures).as("request bodies Jackson can't deserialize").isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Object readJson(String json, Class<?> type) throws Exception {
        HttpMessageConverter<Object> converter = (HttpMessageConverter<Object>) handlerAdapter.getMessageConverters().stream()
                .filter(c -> c.canRead(type, MediaType.APPLICATION_JSON))
                .findFirst().orElseThrow();
        MockHttpInputMessage in = new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8));
        in.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return converter.read((Class<Object>) type, in);
    }

    @SuppressWarnings("unchecked")
    private String toJson(Object body) throws Exception {
        HttpMessageConverter<Object> converter = (HttpMessageConverter<Object>) handlerAdapter.getMessageConverters().stream()
                .filter(c -> c.canWrite(body.getClass(), MediaType.APPLICATION_JSON))
                .findFirst().orElseThrow();
        MockHttpOutputMessage out = new MockHttpOutputMessage();
        converter.write(body, MediaType.APPLICATION_JSON, out);
        return out.getBodyAsString(StandardCharsets.UTF_8);
    }

    @Test
    void transactionSearchPage_keepsFrontendContract() throws Exception {
        TransactionListResponse row = new TransactionListResponse();
        row.setId(5L);
        row.setType(TransactionType.EXPENSE);
        row.setAmount(new BigDecimal("12.50"));
        row.setDateTime(LocalDateTime.of(2026, 11, 13, 10, 30));
        Page<TransactionListResponse> page = new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1);

        String json = toJson(page);

        assertThat(json)
                .contains("\"content\":[")
                .contains("\"totalPages\":1")
                .contains("\"type\":{\"value\":\"EXP\",\"label\":\"Expense\"}")
                .contains("\"dateTime\":\"2026-11-13T10:30:00\"")
                .contains("\"amount\":12.50");
    }

    // Dropdown data is a plain list (frontend reads response.data.categoryList directly), not a Spring Page
    @Test
    void createScreenEnums_arePlainLists() throws Exception {
        CategoryResponse category = new CategoryResponse();
        category.setName("Groceries");
        AccountResponse account = new AccountResponse();
        account.setName("Cash");
        TransactionCreateScreenEnumResponse response = new TransactionCreateScreenEnumResponse(List.of(category), List.of(account));

        String json = toJson(response);

        assertThat(json)
                .contains("\"categoryList\":[{")
                .contains("\"accountList\":[{")
                .doesNotContain("\"content\"")
                .doesNotContain("\"totalPages\"");
    }
}
