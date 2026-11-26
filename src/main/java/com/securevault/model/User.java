package com.securevault.model;

import com.securevault.exception.ValidationException;

import java.time.LocalDateTime;

/**
 * The owner of the vault.
 *
 * The master password itself is NEVER stored. We keep:
 *  - passwordHash + authSalt : used to check the password at login
 *  - keySalt                 : used to turn the password into the encryption key
 */
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;
    private static final int MAX_USERNAME_LENGTH = 32;

    private String username;
    private String passwordHash;
    private String authSalt;
    private String keySalt;

    public User(String username, String passwordHash, String authSalt, String keySalt) {
        super();
        setUsername(username);
        setCredentials(passwordHash, authSalt, keySalt);
    }

    /** Used when loading a saved user. */
    public User(String id, LocalDateTime createdAt, String username,
                String passwordHash, String authSalt, String keySalt) {
        super(id, createdAt);
        setUsername(username);
        setCredentials(passwordHash, authSalt, keySalt);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAuthSalt() {
        return authSalt;
    }

    public String getKeySalt() {
        return keySalt;
    }

    public final void setUsername(String username) {
        String clean = requireText(username, "Username");
        if (clean.length() > MAX_USERNAME_LENGTH) {
            throw new ValidationException("Username must be at most " + MAX_USERNAME_LENGTH + " characters");
        }
        if (!clean.matches("[A-Za-z0-9._-]+")) {
            throw new ValidationException("Username may only contain letters, digits, '.', '_' and '-'");
        }
        this.username = clean;
        touch();
    }

    /** Hash and salts always change together (e.g. when the master password changes). */
    public final void setCredentials(String passwordHash, String authSalt, String keySalt) {
        this.passwordHash = requireText(passwordHash, "Password hash");
        this.authSalt = requireText(authSalt, "Authentication salt");
        this.keySalt = requireText(keySalt, "Key salt");
        touch();
    }

    @Override
    public String getDisplayName() {
        return username;
    }

    @Override
    public String toString() {
        // hash and salts are never printed
        return "User{username='" + username + "', createdAt=" + getCreatedAt() + "}";
    }
}
