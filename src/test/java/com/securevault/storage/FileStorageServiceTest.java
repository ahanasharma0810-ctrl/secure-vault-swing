package com.securevault.storage;

import com.securevault.exception.StorageException;
import com.securevault.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path folder;

    @Test
    void savesAndLoadsUser() throws Exception {
        FileStorageService storage = new FileStorageService(folder);
        assertFalse(storage.userExists());

        User user = new User("demo_user", "hash==", "salt1==", "salt2==");
        storage.saveUser(user);

        assertTrue(storage.userExists());
        User loaded = storage.loadUser();
        assertEquals(user.getId(), loaded.getId());
        assertEquals("demo_user", loaded.getUsername());
        assertEquals("hash==", loaded.getPasswordHash());
        assertEquals("salt2==", loaded.getKeySalt());
    }

    @Test
    void loadingMissingUserFails() {
        assertThrows(StorageException.class, () -> new FileStorageService(folder).loadUser());
    }

    @Test
    void savesAndLoadsVaultBytes() throws Exception {
        FileStorageService storage = new FileStorageService(folder);
        assertNull(storage.loadVault());
        storage.saveVault(new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, storage.loadVault());
    }

    @Test
    void keepsPreviousVaultAsBackup() throws Exception {
        FileStorageService storage = new FileStorageService(folder);
        storage.saveVault(new byte[]{1});
        assertNull(storage.loadVaultBackup());
        storage.saveVault(new byte[]{2});
        assertArrayEquals(new byte[]{2}, storage.loadVault());
        assertArrayEquals(new byte[]{1}, storage.loadVaultBackup());
    }

    @Test
    void rejectsForeignFile() throws Exception {
        FileStorageService storage = new FileStorageService(folder);
        Files.write(folder.resolve("vault.dat"), new byte[]{'X', 'Y', 'Z', 'W', 1});
        assertThrows(StorageException.class, storage::loadVault);
    }

    @Test
    void leavesNoTempFilesBehind() throws Exception {
        FileStorageService storage = new FileStorageService(folder);
        storage.saveVault(new byte[]{1});
        storage.saveUser(new User("demo_user", "h", "s", "k"));
        try (java.util.stream.Stream<Path> files = Files.list(folder)) {
            assertTrue(files.noneMatch(p -> p.toString().endsWith(".tmp")));
        }
    }
}
