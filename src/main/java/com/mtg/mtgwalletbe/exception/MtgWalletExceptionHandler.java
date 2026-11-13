package com.mtg.mtgwalletbe.exception;

import com.mtg.mtgwalletbe.aspect.ServiceLogWriter;
import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class MtgWalletExceptionHandler {
    private final ServiceLogWriter serviceLogWriter;
    static final String CONCURRENT_UPDATE_MESSAGE = "The data was changed by another request. Please try again";
    static final String INVALID_REQUEST_MESSAGE = "Invalid request";
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
        return errorResponse(ex.getMessage(), ex, httpStatus);
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
        return new ResponseEntity<>(ERROR_PREFIX + (details.isEmpty() ? INVALID_REQUEST_MESSAGE : details), HttpStatus.BAD_REQUEST);
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
        return errorResponse(INVALID_REQUEST_MESSAGE, ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleException(UsernameNotFoundException ex) {
        log.warn("User not found");
        return errorResponse(GenericExceptionMessages.USER_NOT_FOUND.getMessage(), ex, GenericExceptionMessages.USER_NOT_FOUND.getStatus());
    }

    /*
     * Concurrent modification of a versioned entity (e.g. two requests changing the same account balance)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleException(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return errorResponse(CONCURRENT_UPDATE_MESSAGE, ex, HttpStatus.CONFLICT);
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
     * The body format the frontend displays: "An error occurred: <message>. <logId>"
     */
    private ResponseEntity<String> errorResponse(String message, Exception ex, HttpStatus status) {
        return new ResponseEntity<>(ERROR_PREFIX + message + ". " + logIdOf(logError(ex)), status);
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
