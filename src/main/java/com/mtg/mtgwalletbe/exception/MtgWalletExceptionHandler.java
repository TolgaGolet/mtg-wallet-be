package com.mtg.mtgwalletbe.exception;

import com.mtg.mtgwalletbe.aspect.ServiceLogWriter;
import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.mtg.mtgwalletbe.aspect.LoggableAspect.MAX_CHARS_RESPONSE;
import static com.mtg.mtgwalletbe.security.SecurityParams.ERROR_CODE_HEADER;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class MtgWalletExceptionHandler {
    private final ServiceLogWriter serviceLogWriter;
    private static final String ERROR_PREFIX = "An error occurred: ";

    /*
     * Expected exception handler. The HTTP status comes from the GenericExceptionMessages constant.
     */
    @ExceptionHandler(MtgWalletGenericException.class)
    public ResponseEntity<String> handleException(MtgWalletGenericException ex) {
        HttpStatus httpStatus = ex.getError().getStatus();
        if (httpStatus.is5xxServerError()) {
            log.error("An error occurred: {}", ex.getMessage(), ex);
        } else {
            log.warn("An error occurred: {}", ex.getMessage());
        }
        return errorResponse(ex.getError(), ex);
    }

    /*
     * Bean validation failures on request bodies: user input, so 400 and no ServiceLog row
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(MtgWalletExceptionHandler::describe)
                .collect(Collectors.joining(", "));
        return validationFailed(details);
    }

    /*
     * The same for method validation, which Spring uses (instead of the above) on handlers that have a constraint
     * directly on a parameter, such as @Min(0) on the search endpoints' pageNo: body errors keep their field details.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<String> handleException(HandlerMethodValidationException ex) {
        String details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> error instanceof FieldError fieldError
                                ? describe(fieldError)
                                : result.getMethodParameter().getParameterName() + " " + error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        return validationFailed(details);
    }

    private ResponseEntity<String> validationFailed(String details) {
        log.warn("Validation failed: {}", details);
        GenericExceptionMessages error = GenericExceptionMessages.INVALID_REQUEST;
        return new ResponseEntity<>(ERROR_PREFIX + (details.isEmpty() ? error.getMessage() : details),
                errorCodeHeaders(error), error.getStatus());
    }

    private static String describe(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    /*
     * Client mistakes that don't carry an HTTP status of their own: malformed JSON, unknown enum values
     * (TransactionType.of(...) etc.), a non-numeric path variable or pageNo. Other IllegalArgumentExceptions are
     * server-side bugs and go to the 500 handler.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, InvalidEnumValueException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return errorResponse(GenericExceptionMessages.INVALID_REQUEST, ex);
    }

    /**
     * Every other client error Spring MVC raises itself implements {@link ErrorResponse} with its status (missing
     * parameter or header, unknown path, wrong method, unsupported or unacceptable media type, ...). A plain 400 is
     * INVALID_REQUEST; any other status is kept, with the framework's headers (e.g. Allow on a 405) and no body.
     * None of them write a ServiceLog row.
     */
    ResponseEntity<String> handleFrameworkClientError(Exception ex, ErrorResponse errorResponse) {
        if (errorResponse.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
            return handleBadRequest(ex);
        }
        log.warn("Client error {}: {}", errorResponse.getStatusCode().value(), ex.getMessage());
        return ResponseEntity.status(errorResponse.getStatusCode()).headers(errorResponse.getHeaders()).build();
    }

    /*
     * Spring Security's "no such user". Only thrown for a missing user; other expected errors must use
     * MtgWalletGenericException, since this handler always answers USER_NOT_FOUND.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleException(UsernameNotFoundException ex) {
        log.warn("User not found");
        return errorResponse(GenericExceptionMessages.USER_NOT_FOUND, ex);
    }

    /*
     * Concurrent modification of a versioned entity (e.g. two requests changing the same account balance)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleException(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return errorResponse(GenericExceptionMessages.CONCURRENT_UPDATE, ex);
    }

    /*
     * Access denied exception from @PreAuthorize handler
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return errorResponse(GenericExceptionMessages.ACCESS_DENIED, ex);
    }

    /*
     * Unexpected exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            return handleFrameworkClientError(ex, errorResponse);
        }
        log.error("An unexpected error occurred", ex);
        // ex.getMessage() for unexpected exceptions is not exposed on purpose
        return new ResponseEntity<>("An unexpected error occurred! " + logIdOf(logError(ex)), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * The body format the frontend displays, with the enum's status. Only server faults (5xx) get a ServiceLog row,
     * and its id is appended ("An error occurred: <message>. <logId>"); expected client errors (4xx) are already in
     * the app log at WARN, so they answer "An error occurred: <message>." without a DB write.
     */
    private ResponseEntity<String> errorResponse(GenericExceptionMessages error, Exception ex) {
        String body = ERROR_PREFIX + error.getMessage() + ".";
        if (error.getStatus().is5xxServerError()) {
            body += " " + logIdOf(logError(ex));
        }
        return new ResponseEntity<>(body, errorCodeHeaders(error), error.getStatus());
    }

    /**
     * X-Error-Code only for client errors (4xx): the UI branches on those, and 5xx constant names are internal.
     */
    private static HttpHeaders errorCodeHeaders(GenericExceptionMessages error) {
        HttpHeaders headers = new HttpHeaders();
        if (error.getStatus().is4xxClientError()) {
            headers.set(ERROR_CODE_HEADER, error.name());
        }
        return headers;
    }

    private ServiceLog logError(Exception ex) {
        String response = ex.getMessage() + " " + ex.getCause() + " " + Arrays.toString(ex.getStackTrace());
        ServiceLog serviceLog = ServiceLog.builder()
                .serviceName(this.getClass().getName())
                .status("E")
                .request(null)
                .response(response.substring(0, Math.min(response.length(), MAX_CHARS_RESPONSE)))
                .startTime(null)
                .endTime(System.currentTimeMillis())
                .executionTime(null)
                .build();
        return serviceLogWriter.save(serviceLog);
    }

    private static String logIdOf(ServiceLog loggedError) {
        return loggedError != null ? String.valueOf(loggedError.getId()) : "-";
    }
}
