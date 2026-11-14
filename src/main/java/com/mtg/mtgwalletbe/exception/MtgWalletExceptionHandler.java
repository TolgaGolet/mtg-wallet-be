package com.mtg.mtgwalletbe.exception;

import com.mtg.mtgwalletbe.aspect.ServiceLogWriter;
import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
        log.warn("Validation failed: {}", details);
        GenericExceptionMessages error = GenericExceptionMessages.INVALID_REQUEST;
        return new ResponseEntity<>(ERROR_PREFIX + (details.isEmpty() ? error.getMessage() : details),
                errorCodeHeaders(error), error.getStatus());
    }

    private static String describe(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    /*
     * Malformed JSON and unknown enum values sent by the client (TransactionType.of(...) etc.). Other
     * IllegalArgumentExceptions are server-side bugs and go to the 500 handler.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, InvalidEnumValueException.class})
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return errorResponse(GenericExceptionMessages.INVALID_REQUEST, ex);
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
        return new ResponseEntity<>("Access denied. " + logIdOf(logError(ex)), HttpStatus.FORBIDDEN);
    }

    /*
     * Unexpected exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("An unexpected error occurred", ex);
        // ex.getMessage() for unexpected exceptions is not exposed on purpose
        return new ResponseEntity<>("An unexpected error occurred! " + logIdOf(logError(ex)), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * The body format the frontend displays: "An error occurred: <message>. <logId>", with the enum's status
     */
    private ResponseEntity<String> errorResponse(GenericExceptionMessages error, Exception ex) {
        String body = ERROR_PREFIX + error.getMessage() + ". " + logIdOf(logError(ex));
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
