package com.securevault.service;

import com.securevault.exception.CryptoException;
import com.securevault.exception.StorageException;
import com.securevault.exception.ValidationException;
import com.securevault.exception.VaultException;
import com.securevault.model.Credential;
import com.securevault.security.EncryptionService;
import com.securevault.storage.CredentialSerializer;
import com.securevault.storage.StorageService;
import com.securevault.util.AppLogger;
import com.securevault.util.InputValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The business logic of the vault: add, edit, delete and search credentials.
 * Every change is encrypted and saved straight away. If saving fails, the
 * change is undone in memory so memory and disk never disagree.
 */
public class VaultService {

    private final StorageService storage;
    private final CredentialSerializer serializer = new CredentialSerializer();
    private EncryptionService encryption;
    private final List<Credential> credentials = new ArrayList<>();
    private boolean recoveredFromBackup = false;

    public VaultService(StorageService storage, EncryptionService encryption) {
        this.storage = storage;
        this.encryption = encryption;
    }

    /**
     * Reads and decrypts the vault from storage. If the vault file is damaged,
     * the backup copy is tried before giving up.
     */
    public void load() throws VaultException {
        credentials.clear();
        recoveredFromBackup = false;
        try {
            credentials.addAll(decode(storage.loadVault()));
        } catch (CryptoException | StorageException mainError) {
            AppLogger.warn("Vault file unreadable, trying backup: " + mainError.getMessage());
            byte[] backup = storage.loadVaultBackup();
            if (backup == null) {
                throw mainError;
            }
            try {
                credentials.addAll(decode(backup));
            } catch (VaultException backupError) {
                throw mainError;
            }
            recoveredFromBackup = true;
            save();   // write the recovered data back as the main file
        }
    }

    private List<Credential> decode(byte[] encrypted) throws VaultException {
        if (encrypted == null) {
            return new ArrayList<>();
        }
        return serializer.fromBytes(encryption.decrypt(encrypted));
    }

    /** True if the last load had to use the backup because the vault was damaged. */
    public boolean wasRecoveredFromBackup() {
        return recoveredFromBackup;
    }

    // ---------------- Reading ----------------

    /** All credentials sorted by website name. */
    public List<Credential> getAll() {
        List<Credential> copy = new ArrayList<>(credentials);
        copy.sort(Comparator.comparing(Credential::getWebsite, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Credential::getUsername, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(copy);
    }

    public List<Credential> search(String keyword) {
        return getAll().stream().filter(c -> c.matches(keyword)).collect(Collectors.toList());
    }

    public Optional<Credential> findById(String id) {
        return credentials.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public int count() {
        return credentials.size();
    }

    /** How many other entries already use this password (to warn about reuse). */
    public int countPasswordReuse(String password, String ignoreId) {
        int count = 0;
        for (Credential c : credentials) {
            if (!c.getId().equals(ignoreId) && c.getPassword().equals(password)) {
                count++;
            }
        }
        return count;
    }

    /** Categories in use, merged with the given defaults, sorted. */
    public List<String> getCategories(String... defaults) {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Collections.addAll(set, defaults);
        credentials.forEach(c -> set.add(c.getCategory()));
        return new ArrayList<>(set);
    }

    // ---------------- Changing ----------------

    public Credential add(String website, String username, String password,
                          String category, String notes) throws VaultException {
        Credential credential = new Credential(InputValidator.normalizeWebsite(website),
                InputValidator.singleLine(username), password, category, notes);
        ensureNotDuplicate(credential, null);
        credentials.add(credential);
        try {
            save();
        } catch (VaultException e) {
            credentials.remove(credential);
            throw e;
        }
        return credential;
    }

    public Credential update(String id, String website, String username, String password,
                             String category, String notes) throws VaultException {
        Credential old = findById(id)
                .orElseThrow(() -> new ValidationException("This entry no longer exists"));
        Credential updated = new Credential(id, old.getCreatedAt(), InputValidator.normalizeWebsite(website),
                InputValidator.singleLine(username), password, category, notes);
        ensureNotDuplicate(updated, id);

        int index = credentials.indexOf(old);
        credentials.set(index, updated);
        try {
            save();
        } catch (VaultException e) {
            credentials.set(index, old);
            throw e;
        }
        return updated;
    }

    public void delete(String id) throws VaultException {
        Credential old = findById(id)
                .orElseThrow(() -> new ValidationException("This entry no longer exists"));
        int index = credentials.indexOf(old);
        credentials.remove(index);
        try {
            save();
        } catch (VaultException e) {
            credentials.add(index, old);
            throw e;
        }
    }

    /** Encrypts the vault with a new key (used when the master password changes). */
    public void reEncrypt(EncryptionService newEncryption) throws VaultException {
        EncryptionService old = this.encryption;
        this.encryption = newEncryption;
        try {
            save();
        } catch (VaultException e) {
            this.encryption = old;
            throw e;
        }
    }

    private void ensureNotDuplicate(Credential candidate, String ignoreId) {
        for (Credential c : credentials) {
            if (!c.getId().equals(ignoreId)
                    && c.getWebsite().equalsIgnoreCase(candidate.getWebsite())
                    && c.getUsername().equalsIgnoreCase(candidate.getUsername())) {
                throw new ValidationException("An entry for " + candidate.getWebsite()
                        + " with username '" + candidate.getUsername() + "' already exists");
            }
        }
    }

    private void save() throws VaultException {
        byte[] plain = serializer.toBytes(credentials);
        storage.saveVault(encryption.encrypt(plain));
    }
}
