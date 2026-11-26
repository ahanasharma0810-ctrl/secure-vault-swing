package com.securevault.service;

import com.securevault.exception.CryptoException;
import com.securevault.exception.ValidationException;
import com.securevault.model.Credential;
import com.securevault.security.AESEncryptionService;
import com.securevault.security.PasswordHasher;
import com.securevault.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VaultServiceTest {

    @TempDir
    Path folder;

    private FileStorageService storage;
    private AESEncryptionService encryption;
    private VaultService vault;

    @BeforeEach
    void setUp() throws Exception {
        storage = new FileStorageService(folder);
        PasswordHasher hasher = new PasswordHasher(1000);
        encryption = new AESEncryptionService(hasher.deriveKey("Master#123".toCharArray(), hasher.generateSalt()));
        vault = new VaultService(storage, encryption);
        vault.load();
    }

    private VaultService reload() throws Exception {
        VaultService fresh = new VaultService(storage, encryption);
        fresh.load();
        return fresh;
    }

    @Test
    void addedEntriesArePersistedEncrypted() throws Exception {
        vault.add("gmail.com", "me@gmail.com", "TopSecret!", "Email", "");
        assertEquals(1, reload().count());

        String fileContent = new String(Files.readAllBytes(folder.resolve("vault.dat")), "ISO-8859-1");
        assertFalse(fileContent.contains("TopSecret!"));
        assertFalse(fileContent.contains("gmail.com"));
    }

    @Test
    void updateChangesFieldsButKeepsId() throws Exception {
        Credential c = vault.add("gmail.com", "me", "old", null, null);
        vault.update(c.getId(), "gmail.com", "me", "new", "Email", "changed");
        Credential stored = reload().findById(c.getId()).orElseThrow(AssertionError::new);
        assertEquals("new", stored.getPassword());
        assertEquals("Email", stored.getCategory());
        assertEquals(c.getCreatedAt(), stored.getCreatedAt());
    }

    @Test
    void deleteRemovesEntry() throws Exception {
        Credential c = vault.add("gmail.com", "me", "pw", null, null);
        vault.delete(c.getId());
        assertEquals(0, reload().count());
        assertThrows(ValidationException.class, () -> vault.delete(c.getId()));
    }

    @Test
    void duplicatesAreRejected() throws Exception {
        vault.add("gmail.com", "me", "pw", null, null);
        assertThrows(ValidationException.class, () -> vault.add("https://www.GMAIL.com/", "ME", "x", null, null));
        assertEquals(1, vault.count());
    }

    @Test
    void websiteIsNormalised() throws Exception {
        Credential c = vault.add("https://www.GitHub.com/login", "me", "pw", null, null);
        assertEquals("github.com", c.getWebsite());
    }

    @Test
    void searchAndSorting() throws Exception {
        vault.add("zoom.us", "a", "1", "Work", null);
        vault.add("amazon.in", "b", "2", "Shopping", null);
        vault.add("github.com", "c", "3", "Work", null);
        List<Credential> all = vault.getAll();
        assertEquals("amazon.in", all.get(0).getWebsite());
        assertEquals(2, vault.search("work").size());
        assertEquals(1, vault.search("amaz").size());
    }

    @Test
    void countsPasswordReuse() throws Exception {
        Credential a = vault.add("a.com", "u", "same", null, null);
        vault.add("b.com", "u", "same", null, null);
        assertEquals(2, vault.countPasswordReuse("same", null));
        assertEquals(1, vault.countPasswordReuse("same", a.getId()));
        assertEquals(0, vault.countPasswordReuse("unique", null));
    }

    @Test
    void categoriesIncludeDefaultsAndUsedOnes() throws Exception {
        vault.add("a.com", "u", "p", "Gaming", null);
        List<String> categories = vault.getCategories("General", "Email");
        assertTrue(categories.contains("Gaming"));
        assertTrue(categories.contains("Email"));
    }

    @Test
    void recoversFromBackupWhenVaultIsDamaged() throws Exception {
        vault.add("a.com", "u", "p", null, null);
        vault.add("b.com", "u", "p2", null, null);   // backup now holds 1 entry, vault 2

        Path file = folder.resolve("vault.dat");
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] ^= 1;   // damage the main file
        Files.write(file, bytes);

        VaultService recovered = reload();
        assertTrue(recovered.wasRecoveredFromBackup());
        assertEquals(1, recovered.count());
        assertFalse(reload().wasRecoveredFromBackup());   // main file was rewritten
    }

    @Test
    void failsWhenVaultAndBackupAreDamaged() throws Exception {
        vault.add("a.com", "u", "p", null, null);
        Path file = folder.resolve("vault.dat");
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] ^= 1;
        Files.write(file, bytes);
        assertThrows(CryptoException.class, this::reload);
    }
}
