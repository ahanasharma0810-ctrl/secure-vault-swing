package com.securevault.security;

import com.securevault.config.AppConfig;
import com.securevault.exception.CryptoException;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * AES-256 in GCM mode.
 *
 * GCM both encrypts and authenticates: if the file is changed or the key is
 * wrong, decryption fails instead of returning garbage.
 * Output format: [12-byte random IV][cipher text + 16-byte tag]
 */
public class AESEncryptionService implements EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public AESEncryptionService(SecretKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        this.key = key;
    }

    @Override
    public byte[] encrypt(byte[] plainData) throws CryptoException {
        if (plainData == null) {
            throw new CryptoException("Nothing to encrypt");
        }
        try {
            byte[] iv = new byte[AppConfig.GCM_IV_LENGTH_BYTES];
            RANDOM.nextBytes(iv);   // a new IV every time - never reuse one

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(AppConfig.GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainData);

            return ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) throws CryptoException {
        int minLength = AppConfig.GCM_IV_LENGTH_BYTES + AppConfig.GCM_TAG_LENGTH_BITS / 8;
        if (encryptedData == null || encryptedData.length < minLength) {
            throw new CryptoException("Encrypted data is too short or missing");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[AppConfig.GCM_IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(AppConfig.GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(cipherText);
        } catch (AEADBadTagException e) {
            throw new CryptoException("Wrong key or the data was tampered with", e);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Decryption failed", e);
        }
    }
}
