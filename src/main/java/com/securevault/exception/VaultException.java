package com.securevault.exception;

/**
 * Base class for every checked exception thrown by Secure Vault.
 * Catching VaultException catches all of the application's own errors.
 */
public class VaultException extends Exception {

    private static final long serialVersionUID = 1L;

    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
