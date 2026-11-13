package com.mtg.mtgwalletbe.exception;

/**
 * An enum value sent by the client (e.g. typeValue) that matches no constant. Mapped to 400; other
 * IllegalArgumentExceptions are server-side bugs and stay 500.
 */
public class InvalidEnumValueException extends IllegalArgumentException {
    public InvalidEnumValueException(String value) {
        super("Invalid enum value: " + value);
    }
}
