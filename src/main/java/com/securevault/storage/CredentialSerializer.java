package com.securevault.storage;

import com.securevault.exception.StorageException;
import com.securevault.model.Credential;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a list of credentials to bytes and back.
 * The bytes are encrypted afterwards, so this class never touches the disk.
 *
 * Format: [format version][count] then for each credential its fields in order.
 */
public class CredentialSerializer {

    private static final int FORMAT_VERSION = 1;

    public byte[] toBytes(List<Credential> credentials) throws StorageException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(FORMAT_VERSION);
            out.writeInt(credentials.size());
            for (Credential c : credentials) {
                out.writeUTF(c.getId());
                out.writeUTF(c.getCreatedAt().toString());
                out.writeUTF(c.getWebsite());
                out.writeUTF(c.getUsername());
                out.writeUTF(c.getPassword());
                out.writeUTF(c.getCategory());
                out.writeUTF(c.getNotes());
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new StorageException("Could not convert the vault to bytes", e);
        }
    }

    public List<Credential> fromBytes(byte[] data) throws StorageException {
        List<Credential> list = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new StorageException("Unsupported vault format version: " + version);
            }
            int count = in.readInt();
            if (count < 0) {
                throw new StorageException("Vault data is corrupted");
            }
            for (int i = 0; i < count; i++) {
                String id = in.readUTF();
                LocalDateTime createdAt = LocalDateTime.parse(in.readUTF());
                String website = in.readUTF();
                String username = in.readUTF();
                String password = in.readUTF();
                String category = in.readUTF();
                String notes = in.readUTF();
                list.add(new Credential(id, createdAt, website, username, password, category, notes));
            }
            return list;
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            throw new StorageException("Vault data is corrupted", e);
        }
    }
}
