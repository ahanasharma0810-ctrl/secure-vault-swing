package com.securevault.exception;

/** Thrown when the username or master password is wrong. */
public class AuthenticationException extends VaultException {

    private static final long serialVersionUID = 1L;

    private final int attemptsRemaining;

    public AuthenticationException(String message, int attemptsRemaining) {
        super(message);
        this.attemptsRemaining = attemptsRemaining;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }
}
