package com.securevault.service;

import com.securevault.config.AppConfig;
import com.securevault.exception.AccountLockedException;
import com.securevault.exception.AuthenticationException;
import com.securevault.exception.ValidationException;
import com.securevault.security.PasswordHasher;
import com.securevault.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private static final char[] GOOD = "Strong#Pass1".toCharArray();

    @TempDir
    Path folder;

    private FileStorageService storage;
    private final AtomicLong clock = new AtomicLong(1_000_000);
    private AuthService auth;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(folder);
        auth = new AuthService(storage, new PasswordHasher(1000), clock::get);
    }

    @Test
    void registerThenLogin() throws Exception {
        assertFalse(auth.isRegistered());
        Session session = auth.register("demo_user", GOOD, GOOD);
        assertTrue(auth.isRegistered());
        assertEquals("demo_user", session.getUser().getUsername());

        Session login = auth.login("DEMO_USER", GOOD);   // username is not case sensitive
        assertEquals(session.getUser().getId(), login.getUser().getId());
    }

    @Test
    void registerRejectsBadPasswords() {
        assertThrows(ValidationException.class, () -> auth.register("demo_user", "Sh0rt!".toCharArray(), "Sh0rt!".toCharArray()));
        assertThrows(ValidationException.class, () -> auth.register("demo_user", GOOD, "Different#1".toCharArray()));
        assertThrows(ValidationException.class, () -> auth.register("demo_user", "password".toCharArray(), "password".toCharArray()));
    }

    @Test
    void cannotRegisterTwice() throws Exception {
        auth.register("demo_user", GOOD, GOOD);
        assertThrows(ValidationException.class, () -> auth.register("other", GOOD, GOOD));
    }

    @Test
    void wrongPasswordCountsAttempts() throws Exception {
        auth.register("demo_user", GOOD, GOOD);
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> auth.login("demo_user", "Wrong#Pass1".toCharArray()));
        assertEquals(AppConfig.MAX_LOGIN_ATTEMPTS - 1, e.getAttemptsRemaining());
        assertThrows(AuthenticationException.class, () -> auth.login("someoneelse", GOOD));
    }

    @Test
    void locksAfterTooManyAttemptsAndUnlocksLater() throws Exception {
        auth.register("demo_user", GOOD, GOOD);
        for (int i = 0; i < AppConfig.MAX_LOGIN_ATTEMPTS - 1; i++) {
            assertThrows(AuthenticationException.class, () -> auth.login("demo_user", "Wrong#1234".toCharArray()));
        }
        assertThrows(AccountLockedException.class, () -> auth.login("demo_user", "Wrong#1234".toCharArray()));
        // even the right password is refused while locked
        assertThrows(AccountLockedException.class, () -> auth.login("demo_user", GOOD));

        clock.addAndGet(AppConfig.LOCKOUT_SECONDS * 1000L + 1);
        assertNotNull(auth.login("demo_user", GOOD));
    }

    @Test
    void emptyLoginIsValidationError() throws Exception {
        auth.register("demo_user", GOOD, GOOD);
        assertThrows(ValidationException.class, () -> auth.login("", GOOD));
        assertThrows(ValidationException.class, () -> auth.login("demo_user", new char[0]));
    }

    @Test
    void changeMasterPasswordReEncryptsVault() throws Exception {
        Session session = auth.register("demo_user", GOOD, GOOD);
        VaultService vault = new VaultService(storage, session.getEncryptionService());
        vault.add("gmail.com", "me", "pw1", null, null);

        char[] newPw = "Brand#New2026".toCharArray();
        auth.changeMasterPassword(session, vault, GOOD, newPw, newPw);

        assertThrows(AuthenticationException.class, () -> auth.login("demo_user", GOOD));
        Session fresh = auth.login("demo_user", newPw);
        VaultService reloaded = new VaultService(storage, fresh.getEncryptionService());
        reloaded.load();
        assertEquals(1, reloaded.count());
    }

    @Test
    void changeMasterPasswordNeedsCurrentPassword() throws Exception {
        Session session = auth.register("demo_user", GOOD, GOOD);
        VaultService vault = new VaultService(storage, session.getEncryptionService());
        char[] newPw = "Brand#New2026".toCharArray();
        assertThrows(AuthenticationException.class,
                () -> auth.changeMasterPassword(session, vault, "Wrong#Pass1".toCharArray(), newPw, newPw));
        assertThrows(ValidationException.class,
                () -> auth.changeMasterPassword(session, vault, GOOD, GOOD, GOOD));
    }
}
