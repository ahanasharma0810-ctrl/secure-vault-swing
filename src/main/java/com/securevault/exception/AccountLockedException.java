package com.securevault.exception;

/** Thrown when too many wrong passwords were entered and login is blocked for a while. */
public class AccountLockedException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    private final long secondsRemaining;

    public AccountLockedException(long secondsRemaining) {
        super("Too many failed attempts. Try again in " + secondsRemaining + " seconds.", 0);
        this.secondsRemaining = secondsRemaining;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }
}
