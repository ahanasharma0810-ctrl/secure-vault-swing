package com.securevault.storage;

import com.securevault.exception.StorageException;
import com.securevault.model.Credential;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CredentialSerializerTest {

    private final CredentialSerializer serializer = new CredentialSerializer();

    @Test
    void roundTripKeepsAllFields() throws Exception {
        Credential a = new Credential("gmail.com", "me@gmail.com", "p@ss", "Email", "line1\nline2");
        Credential b = new Credential("बैंक app", "user", "पासवर्ड", null, "");   // unicode works
        List<Credential> back = serializer.fromBytes(serializer.toBytes(Arrays.asList(a, b)));

        assertEquals(2, back.size());
        Credential first = back.get(0);
        assertEquals(a.getId(), first.getId());
        assertEquals(a.getCreatedAt(), first.getCreatedAt());
        assertEquals(a.getWebsite(), first.getWebsite());
        assertEquals(a.getPassword(), first.getPassword());
        assertEquals(a.getNotes(), first.getNotes());
        assertEquals("पासवर्ड", back.get(1).getPassword());
    }

    @Test
    void emptyList() throws Exception {
        assertTrue(serializer.fromBytes(serializer.toBytes(Collections.emptyList())).isEmpty());
    }

    @Test
    void garbageIsRejected() {
        assertThrows(StorageException.class, () -> serializer.fromBytes(new byte[]{0, 0, 0, 1, 0, 0, 0, 5}));
        assertThrows(StorageException.class, () -> serializer.fromBytes(new byte[]{9, 9, 9, 9}));
    }
}
