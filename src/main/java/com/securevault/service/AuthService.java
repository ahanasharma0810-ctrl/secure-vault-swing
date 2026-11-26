package com.securevault.service;

import com.securevault.config.AppConfig;
import com.securevault.exception.AccountLockedException;
import com.securevault.exception.AuthenticationException;
import com.securevault.exception.StorageException;
import com.securevault.exception.ValidationException;
import com.securevault.exception.VaultException;
import com.securevault.model.PasswordStrength;
import com.securevault.model.User;
import com.securevault.security.AESEncryptionService;
import com.securevault.security.PasswordHasher;
import com.securevault.security.PasswordStrengthChecker;
import com.securevault.storage.StorageService;

import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * Handles the master account: registering, logging in (with lock-out after
 * too many wrong attempts) and changing the master password.
 */
public class AuthService {

    private final StorageService storage;
    private final PasswordHasher hasher;
    private final PasswordStrengthChecker strengthChecker = new PasswordStrengthChecker();
    private final LongSupplier clock;

    private int failedAttempts;
    private long lockedUntilMillis;

    public AuthService(StorageService storage) {
        this(storage, new PasswordHasher(), System::currentTimeMillis);
    }

    /** Lets tests pass a fast hasher and a fake clock. */
    public AuthService(StorageService storage, PasswordHasher hasher, LongSupplier clock) {
        this.storage = storage;
        this.hasher = hasher;
        this.clock = clock;
    }

    public boolean isRegistered() {
        return storage.userExists();
    }

    /** Creates the master account and returns a logged-in session. */
    public Session register(String username, char[] password, char[] confirm) throws VaultException {
        if (isRegistered()) {
            throw new ValidationException("An account already exists on this computer");
        }
        validateNewPassword(password, confirm);

        byte[] authSalt = hasher.generateSalt();
        byte[] keySalt = hasher.generateSalt();
        byte[] hash = hasher.hash(password, authSalt);

        User user = new User(username,
                PasswordHasher.toBase64(hash),
                PasswordHasher.toBase64(authSalt),
                PasswordHasher.toBase64(keySalt));
        storage.saveUser(user);

        return new Session(user, new AESEncryptionService(hasher.deriveKey(password, keySalt)));
    }

    /** Checks the username and password. Throws if wrong or locked. */
    public Session login(String username, char[] password) throws VaultException {
        long now = clock.getAsLong();
        if (now < lockedUntilMillis) {
            long seconds = (lockedUntilMillis - now + 999) / 1000;
            throw new AccountLockedException(seconds);
        }
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            throw new ValidationException("Please enter your username and master password");
        }

        User user = storage.loadUser();
        boolean nameOk = user.getUsername().equalsIgnoreCase(username.trim());
        boolean passwordOk = hasher.verify(password,
                PasswordHasher.fromBase64(user.getAuthSalt()),
                PasswordHasher.fromBase64(user.getPasswordHash()));

        if (!nameOk || !passwordOk) {
            failedAttempts++;
            if (failedAttempts >= AppConfig.MAX_LOGIN_ATTEMPTS) {
                failedAttempts = 0;
                lockedUntilMillis = now + AppConfig.LOCKOUT_SECONDS * 1000L;
                throw new AccountLockedException(AppConfig.LOCKOUT_SECONDS);
            }
            int left = AppConfig.MAX_LOGIN_ATTEMPTS - failedAttempts;
            throw new AuthenticationException("Wrong username or master password. "
                    + left + " attempt(s) left.", left);
        }

        failedAttempts = 0;
        return new Session(user, new AESEncryptionService(
                hasher.deriveKey(password, PasswordHasher.fromBase64(user.getKeySalt()))));
    }

    /**
     * Changes the master password: the vault is re-encrypted with a new key
     * and the account gets a new hash and new salts.
     */
    public Session changeMasterPassword(Session session, VaultService vault, char[] currentPassword,
                                        char[] newPassword, char[] confirm) throws VaultException {
        User user = session.getUser();
        boolean currentOk = hasher.verify(currentPassword,
                PasswordHasher.fromBase64(user.getAuthSalt()),
                PasswordHasher.fromBase64(user.getPasswordHash()));
        if (!currentOk) {
            throw new AuthenticationException("The current master password is wrong", 0);
        }
        if (Arrays.equals(currentPassword, newPassword)) {
            throw new ValidationException("The new password must be different from the current one");
        }
        validateNewPassword(newPassword, confirm);

        byte[] authSalt = hasher.generateSalt();
        byte[] keySalt = hasher.generateSalt();
        byte[] hash = hasher.hash(newPassword, authSalt);
        AESEncryptionService newEncryption = new AESEncryptionService(hasher.deriveKey(newPassword, keySalt));

        String oldHash = user.getPasswordHash();
        String oldAuthSalt = user.getAuthSalt();
        String oldKeySalt = user.getKeySalt();

        // 1. re-encrypt the vault with the new key
        vault.reEncrypt(newEncryption);
        // 2. save the new hash and salts; undo step 1 if this fails
        try {
            user.setCredentials(PasswordHasher.toBase64(hash),
                    PasswordHasher.toBase64(authSalt), PasswordHasher.toBase64(keySalt));
            storage.saveUser(user);
        } catch (StorageException e) {
            user.setCredentials(oldHash, oldAuthSalt, oldKeySalt);
            vault.reEncrypt(session.getEncryptionService());
            throw e;
        }
        return new Session(user, newEncryption);
    }

    /** Rules for a new master password. */
    public void validateNewPassword(char[] password, char[] confirm) {
        if (password == null || password.length < AppConfig.MIN_MASTER_PASSWORD_LENGTH) {
            throw new ValidationException("Master password must be at least "
                    + AppConfig.MIN_MASTER_PASSWORD_LENGTH + " characters");
        }
        if (!Arrays.equals(password, confirm)) {
            throw new ValidationException("Passwords do not match");
        }
        PasswordStrength strength = strengthChecker.evaluate(new String(password));
        if (strength.getScore() < PasswordStrength.MEDIUM.getScore()) {
            throw new ValidationException("Master password is too weak (" + strength.getLabel()
                    + "). Mix upper and lower case letters, digits and symbols.");
        }
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }
}
