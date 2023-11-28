package com.mtg.mtgwalletbe.exception;

import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.repository.ServiceLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class MtgWalletExceptionHandler {
    private final ServiceLogRepository serviceLogRepository;
    private static final int MAX_CHARS_RESPONSE = 1000;

    /*
     * Expected exception handler
     */
    @ExceptionHandler(MtgWalletGenericException.class)
    public ResponseEntity<String> handleException(MtgWalletGenericException ex) {
        log.error("An error occurred: " + ex.getMessage() + Arrays.toString(ex.getStackTrace()));
        logError(ex);
        String errorMessage = "An error occurred: " + ex.getMessage();
        // Returned HTTP status codes for exceptions are managed from here
        HttpStatus httpStatus = switch (GenericExceptionMessages.fromMessage(ex.getMessage())) {
            case USERNAME_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case BAD_USERNAME_OR_PASSWORD, JWT_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case null, default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return new ResponseEntity<>(errorMessage, httpStatus);
    }

    /*
     * Unexpected exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("An unexpected error occurred: " + ex.getMessage() + Arrays.toString(ex.getStackTrace()));
        logError(ex);
        // ex.getMessage() for unexpected exceptions is not exposed on purpose
        return new ResponseEntity<>("An unexpected error occurred!", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logError(Exception ex) {
        String response = ex.getMessage() + Arrays.toString(ex.getStackTrace());
        ServiceLog serviceLog = ServiceLog.builder()
                .serviceName(this.getClass().getName())
                .status("E")
                .request(null)
                .response(response.substring(0, Math.min(response.length(), MAX_CHARS_RESPONSE)))
                .startTime(null)
                .endTime(System.currentTimeMillis())
                .executionTime(null)
                .build();
        serviceLogRepository.save(serviceLog);
    }
}
