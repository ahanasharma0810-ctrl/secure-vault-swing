package com.securevault.exception;

/**
 * Thrown when user input is invalid (empty field, short password, ...).
 * It is unchecked so model setters can throw it without forcing try/catch everywhere.
 */
public class ValidationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
