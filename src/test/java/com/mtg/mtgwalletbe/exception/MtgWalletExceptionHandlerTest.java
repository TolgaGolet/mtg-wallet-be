package com.mtg.mtgwalletbe.exception;

import com.mtg.mtgwalletbe.aspect.ServiceLogWriter;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MtgWalletExceptionHandlerTest {

    private ServiceLogWriter serviceLogWriter;
    private MtgWalletExceptionHandler handler;

    @BeforeEach
    void setUp() {
        serviceLogWriter = mock(ServiceLogWriter.class);
        when(serviceLogWriter.save(any(ServiceLog.class))).thenAnswer(invocation -> {
            ServiceLog log = invocation.getArgument(0);
            log.setId(42L);
            return log;
        });
        handler = new MtgWalletExceptionHandler(serviceLogWriter);
    }

    // Regression for CQ-04: the status comes from the error constant, not from a message lookup
    @ParameterizedTest
    @CsvSource({
            "ACCOUNT_NOT_FOUND, NOT_FOUND",
            "ACCOUNT_NAME_ALREADY_EXISTS, CONFLICT",
            "ACCOUNTS_LIMIT_EXCEEDED, BAD_REQUEST",
            "WRONG_PASSWORD, BAD_REQUEST",
            "INVALID_TOTP_CODE, BAD_REQUEST",
            "JWT_EXPIRED, UNAUTHORIZED",
            "NOT_VERIFIED_EMAIL, BAD_REQUEST",
            "CATEGORY_HAS_ACTIVE_SUBCATEGORIES, CONFLICT",
            "DATABASE_DOWN, SERVICE_UNAVAILABLE",
            "ROLE_NOT_FOUND, INTERNAL_SERVER_ERROR"
    })
    void genericException_usesStatusOfErrorConstant(GenericExceptionMessages error, HttpStatus expected) {
        ResponseEntity<String> response = handler.handleException(new MtgWalletGenericException(error));

        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody()).isEqualTo("An error occurred: " + error.getMessage() + ". 42");
    }

    // The frontend refreshes the token and retries on any 401/403, so user-input errors must never use them
    @Test
    void userInputErrors_areNeverUnauthorizedOrForbidden() {
        for (GenericExceptionMessages error : new GenericExceptionMessages[]{
                GenericExceptionMessages.WRONG_PASSWORD, GenericExceptionMessages.PASSWORDS_MISMATCH,
                GenericExceptionMessages.INVALID_TOTP_CODE, GenericExceptionMessages.BAD_USERNAME_OR_PASSWORD,
                GenericExceptionMessages.NOT_VERIFIED_EMAIL, GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM}) {
            assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    void validationError_returnsBadRequestWithFieldMessage_andNoServiceLogRow() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must be greater than or equal to 0.01"));
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("toString"), -1);

        ResponseEntity<String> response = handler.handleException(new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("An error occurred: amount must be greater than or equal to 0.01");
        verifyNoInteractions(serviceLogWriter);
    }

    @Test
    void invalidEnumValue_returnsBadRequest() {
        ResponseEntity<String> response = handler.handleBadRequest(new InvalidEnumValueException("XYZ"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).startsWith("An error occurred: Invalid request");
    }

    // Regression: every IllegalArgumentException used to be a 400, hiding server bugs as client errors
    @Test
    void plainIllegalArgumentException_isNotMappedToBadRequest() throws Exception {
        ExceptionHandler mapping = MtgWalletExceptionHandler.class.getMethod("handleBadRequest", Exception.class).getAnnotation(ExceptionHandler.class);

        assertThat(mapping.value()).doesNotContain(IllegalArgumentException.class).contains(InvalidEnumValueException.class);
    }

    @Test
    void enumFactory_throwsInvalidEnumValue() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> com.mtg.mtgwalletbe.enums.TransactionType.of("XYZ"))
                .isInstanceOf(InvalidEnumValueException.class);
    }

    // Regression for CQ-01: a concurrent account write must surface as 409, not 500
    @Test
    void optimisticLockingFailure_returnsConflict() {
        ResponseEntity<String> response = handler.handleException(
                new ObjectOptimisticLockingFailureException(Account.class, 1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .contains(MtgWalletExceptionHandler.CONCURRENT_UPDATE_MESSAGE)
                .endsWith("42");
    }

    // Regression for CQ-14: if the log row can't be saved, the client still gets the real error
    @Test
    void logWriteFailure_stillReturnsOriginalError() {
        when(serviceLogWriter.save(any(ServiceLog.class))).thenReturn(null);

        ResponseEntity<String> response = handler.handleException(new MtgWalletGenericException(GenericExceptionMessages.ACCOUNT_NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("An error occurred: Account not found. -");
    }

    @Test
    void unexpectedException_hidesMessage() {
        ResponseEntity<String> response = handler.handleException(new RuntimeException("db password leaked in message"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("An unexpected error occurred! 42").doesNotContain("password");
    }
}
