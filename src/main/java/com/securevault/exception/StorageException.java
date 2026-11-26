package com.securevault.exception;

/** Thrown when reading or writing data files fails. */
public class StorageException extends VaultException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
