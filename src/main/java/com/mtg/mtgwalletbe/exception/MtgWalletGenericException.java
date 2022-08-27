package com.mtg.mtgwalletbe.exception;

public class MtgWalletGenericException extends Exception {
    public MtgWalletGenericException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public MtgWalletGenericException(String errorMessage) {
        super(errorMessage);
    }
}
