package com.securevault.exception;

/** Thrown when encryption, decryption or hashing fails. */
public class CryptoException extends VaultException {

    private static final long serialVersionUID = 1L;

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
