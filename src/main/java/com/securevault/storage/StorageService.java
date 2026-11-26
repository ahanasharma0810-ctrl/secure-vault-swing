package com.securevault.storage;

import com.securevault.exception.StorageException;
import com.securevault.model.User;

/**
 * Abstraction for where the data is kept (files, database, ...).
 * Storage only deals with bytes and the account record; it never sees
 * plain-text passwords because the vault is encrypted before it arrives here.
 */
public interface StorageService {

    /** @return true if a master account has already been created. */
    boolean userExists();

    void saveUser(User user) throws StorageException;

    User loadUser() throws StorageException;

    /** @return true if an encrypted vault has been saved before. */
    boolean vaultExists();

    void saveVault(byte[] encryptedVault) throws StorageException;

    byte[] loadVault() throws StorageException;

    /**
     * The previous copy of the vault, kept in case the current one is damaged.
     * Implementations without backups simply return null.
     */
    default byte[] loadVaultBackup() throws StorageException {
        return null;
    }
}
