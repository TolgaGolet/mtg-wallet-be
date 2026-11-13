package com.mtg.mtgwalletbe.exception;

import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import lombok.Getter;

/**
 * Expected business error. The enum constant carries both the user-facing message and the HTTP status.
 */
@Getter
public class MtgWalletGenericException extends Exception {
    private final GenericExceptionMessages error;

    public MtgWalletGenericException(GenericExceptionMessages error) {
        super(error.getMessage());
        this.error = error;
    }

    public MtgWalletGenericException(GenericExceptionMessages error, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
    }
}
