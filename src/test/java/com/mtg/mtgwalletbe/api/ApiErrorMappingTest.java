package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.aspect.ServiceLogWriter;
import com.mtg.mtgwalletbe.config.ApplicationConfigProperties;
import com.mtg.mtgwalletbe.exception.MtgWalletExceptionHandler;
import com.mtg.mtgwalletbe.mapper.TransactionServiceMapper;
import com.mtg.mtgwalletbe.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.mtg.mtgwalletbe.security.SecurityParams.ERROR_CODE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression for F2: Spring MVC's own client errors fell through to the catch-all 500 handler.
// Web layer only: the real controller and exception handler, mocked services, no security filters.
class ApiErrorMappingTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final ServiceLogWriter serviceLogWriter = mock(ServiceLogWriter.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ApplicationConfigProperties properties = mock(ApplicationConfigProperties.class);
        when(properties.getDefaultPageSize()).thenReturn(10);
        when(transactionService.search(any(), any())).thenReturn(Page.empty());
        TransactionApi api = new TransactionApi(properties, transactionService, mock(TransactionServiceMapper.class));
        mvc = MockMvcBuilders.standaloneSetup(api).setControllerAdvice(new MtgWalletExceptionHandler(serviceLogWriter)).build();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder search(String pageNo) {
        return post("/transaction/search").param("pageNo", pageNo).contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    @Test
    void nonNumericPathVariable_is400() throws Exception {
        mvc.perform(delete("/transaction/delete/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_REQUEST"));
    }

    @Test
    void nonNumericPageNo_is400() throws Exception {
        mvc.perform(search("abc")).andExpect(status().isBadRequest());
    }

    @Test
    void negativePageNo_is400() throws Exception {
        mvc.perform(search("-1"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_REQUEST"));
        verifyNoInteractions(transactionService);
    }

    // Regression: the 405 answer dropped the framework's Allow header
    @Test
    void wrongMethod_is405_withAllowHeader() throws Exception {
        mvc.perform(get("/transaction/search"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"));
    }

    // Regression: framework client errors outside a hand-picked list (here 406) became 500 with a ServiceLog row
    @Test
    void unacceptableMediaType_keepsItsStatus() throws Exception {
        mvc.perform(search("0").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        verifyNoInteractions(serviceLogWriter);
    }

    // Regression: with @Min on pageNo, Spring validates the body through method validation, and its errors lost
    // the field details ("Invalid request." instead of what was wrong)
    @Test
    void invalidSearchBody_keepsFieldDetails() throws Exception {
        mvc.perform(post("/transaction/search").param("pageNo", "0").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ERROR_CODE_HEADER, "INVALID_REQUEST"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("notes")));
        verifyNoInteractions(transactionService);
    }

    @Test
    void clientErrors_writeNoServiceLogRows() throws Exception {
        mvc.perform(delete("/transaction/delete/abc"));
        mvc.perform(search("-1"));
        mvc.perform(get("/transaction/search"));

        verifyNoInteractions(serviceLogWriter);
    }

    // Nitpick: rows with equal timestamps must not move between pages, so id breaks ties
    @Test
    void search_sortsByDateTimeThenId_newestFirst() throws Exception {
        mvc.perform(search("2")); // only the Pageable handed to the service matters here

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionService).search(any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getSort()).containsExactly(Sort.Order.desc("dateTime"), Sort.Order.desc("id"));
    }
}
