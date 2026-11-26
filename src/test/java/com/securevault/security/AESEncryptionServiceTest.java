package com.securevault.security;

import com.securevault.exception.CryptoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AESEncryptionServiceTest {

    private final PasswordHasher hasher = new PasswordHasher(1000);
    private final byte[] salt = hasher.generateSalt();

    private AESEncryptionService serviceFor(String password) throws CryptoException {
        return new AESEncryptionService(hasher.deriveKey(password.toCharArray(), salt));
    }

    @Test
    void encryptThenDecryptGivesOriginal() throws Exception {
        AESEncryptionService aes = serviceFor("Master#Pass1");
        byte[] original = "my secret data".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(original, aes.decrypt(aes.encrypt(original)));
    }

    @Test
    void textHelpersWork() throws Exception {
        AESEncryptionService aes = serviceFor("Master#Pass1");
        assertEquals("hello world", aes.decryptText(aes.encryptText("hello world")));
    }

    @Test
    void sameInputEncryptsDifferentlyEachTime() throws Exception {
        AESEncryptionService aes = serviceFor("Master#Pass1");
        assertNotEquals(aes.encryptText("same"), aes.encryptText("same"));   // random IV
    }

    @Test
    void wrongKeyFails() throws Exception {
        String encrypted = serviceFor("Master#Pass1").encryptText("secret");
        assertThrows(CryptoException.class, () -> serviceFor("Wrong#Pass1").decryptText(encrypted));
    }

    @Test
    void tamperedDataFails() throws Exception {
        AESEncryptionService aes = serviceFor("Master#Pass1");
        byte[] encrypted = aes.encrypt("secret".getBytes(StandardCharsets.UTF_8));
        encrypted[encrypted.length - 1] ^= 1;
        assertThrows(CryptoException.class, () -> aes.decrypt(encrypted));
    }

    @Test
    void tooShortDataFails() throws Exception {
        AESEncryptionService aes = serviceFor("Master#Pass1");
        assertThrows(CryptoException.class, () -> aes.decrypt(new byte[5]));
    }
}
