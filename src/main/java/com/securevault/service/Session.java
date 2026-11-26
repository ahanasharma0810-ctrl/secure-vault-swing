package com.securevault.service;

import com.securevault.model.User;
import com.securevault.security.EncryptionService;

/**
 * What a successful login produces: the logged-in user and the
 * encryption service built from their master password.
 * It only lives in memory and is thrown away when the vault locks.
 */
public final class Session {

    private final User user;
    private final EncryptionService encryptionService;

    public Session(User user, EncryptionService encryptionService) {
        this.user = user;
        this.encryptionService = encryptionService;
    }

    public User getUser() {
        return user;
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }
}
