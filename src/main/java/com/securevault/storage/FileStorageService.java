package com.securevault.storage;

import com.securevault.config.AppConfig;
import com.securevault.exception.StorageException;
import com.securevault.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Properties;

/**
 * Stores data in files inside a folder (by default ~/.securevault):
 *  - user.properties : username, password hash and salts (no password)
 *  - vault.dat       : the AES-encrypted credentials
 *
 * Files are written to a temporary file first and then moved into place,
 * so a crash in the middle of saving never leaves a half-written file.
 */
public class FileStorageService implements StorageService {

    /** First bytes of every vault file, used to recognise it. */
    private static final byte[] MAGIC = {'S', 'V', 'L', 'T'};

    private final Path folder;
    private final Path userFile;
    private final Path vaultFile;
    private final Path backupFile;

    public FileStorageService() {
        this(Paths.get(AppConfig.DATA_FOLDER));
    }

    public FileStorageService(Path folder) {
        this.folder = folder;
        this.userFile = folder.resolve(AppConfig.USER_FILE_NAME);
        this.vaultFile = folder.resolve(AppConfig.VAULT_FILE_NAME);
        this.backupFile = folder.resolve(AppConfig.VAULT_FILE_NAME + ".bak");
    }

    public Path getFolder() {
        return folder;
    }

    // ---------------- User ----------------

    @Override
    public boolean userExists() {
        return Files.isRegularFile(userFile);
    }

    @Override
    public void saveUser(User user) throws StorageException {
        Properties props = new Properties();
        props.setProperty("id", user.getId());
        props.setProperty("createdAt", user.getCreatedAt().toString());
        props.setProperty("username", user.getUsername());
        props.setProperty("passwordHash", user.getPasswordHash());
        props.setProperty("authSalt", user.getAuthSalt());
        props.setProperty("keySalt", user.getKeySalt());

        Path temp = prepareTempFile(userFile);
        try (OutputStream out = Files.newOutputStream(temp)) {
            props.store(out, AppConfig.APP_NAME + " account - do not edit");
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new StorageException("Could not save the account", e);
        }
        moveIntoPlace(temp, userFile);
    }

    @Override
    public User loadUser() throws StorageException {
        if (!userExists()) {
            throw new StorageException("No account found. Please register first.");
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(userFile)) {
            props.load(in);
            return new User(
                    props.getProperty("id"),
                    LocalDateTime.parse(props.getProperty("createdAt")),
                    props.getProperty("username"),
                    props.getProperty("passwordHash"),
                    props.getProperty("authSalt"),
                    props.getProperty("keySalt"));
        } catch (IOException | RuntimeException e) {
            throw new StorageException("The account file is damaged", e);
        }
    }

    // ---------------- Vault ----------------

    @Override
    public boolean vaultExists() {
        return Files.isRegularFile(vaultFile);
    }

    @Override
    public void saveVault(byte[] encryptedVault) throws StorageException {
        byte[] content = new byte[MAGIC.length + encryptedVault.length];
        System.arraycopy(MAGIC, 0, content, 0, MAGIC.length);
        System.arraycopy(encryptedVault, 0, content, MAGIC.length, encryptedVault.length);

        Path temp = prepareTempFile(vaultFile);
        try {
            Files.write(temp, content);
            if (Files.isRegularFile(vaultFile)) {
                // keep the last good copy in case the new one gets damaged later
                Files.copy(vaultFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new StorageException("Could not save the vault", e);
        }
        moveIntoPlace(temp, vaultFile);
    }

    @Override
    public byte[] loadVaultBackup() throws StorageException {
        if (!Files.isRegularFile(backupFile)) {
            return null;
        }
        try {
            return stripMagic(Files.readAllBytes(backupFile));
        } catch (IOException e) {
            throw new StorageException("Could not read the vault backup", e);
        }
    }

    @Override
    public byte[] loadVault() throws StorageException {
        if (!vaultExists()) {
            return null;   // nothing saved yet
        }
        try {
            return stripMagic(Files.readAllBytes(vaultFile));
        } catch (IOException e) {
            throw new StorageException("Could not read the vault", e);
        }
    }

    // ---------------- Helpers ----------------

    protected static byte[] stripMagic(byte[] content) throws StorageException {
        if (content.length < MAGIC.length
                || !Arrays.equals(Arrays.copyOfRange(content, 0, MAGIC.length), MAGIC)) {
            throw new StorageException("This is not a Secure Vault file");
        }
        return Arrays.copyOfRange(content, MAGIC.length, content.length);
    }

    private Path prepareTempFile(Path target) throws StorageException {
        try {
            Files.createDirectories(folder);
            return Files.createTempFile(folder, target.getFileName().toString(), ".tmp");
        } catch (IOException e) {
            throw new StorageException("Cannot write to the data folder: " + folder, e);
        }
    }

    private static void moveIntoPlace(Path temp, Path target) throws StorageException {
        try {
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new StorageException("Could not finish saving " + target.getFileName(), e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // nothing more we can do
        }
    }
}
