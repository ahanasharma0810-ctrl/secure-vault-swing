package com.securevault.service;

import com.securevault.model.Credential;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAnalyzerTest {

    private final SecurityAnalyzer analyzer = new SecurityAnalyzer();

    @Test
    void emptyVaultHasNoIssues() {
        SecurityReport report = analyzer.analyze(Collections.emptyList());
        assertEquals(0, report.getTotal());
        assertFalse(report.hasIssues());
    }

    @Test
    void findsWeakPasswords() {
        Credential weak = new Credential("spotify.com", "u", "sunshine", null, null);
        Credential strong = new Credential("gmail.com", "u", "Gm@il#Secure2026", null, null);
        SecurityReport report = analyzer.analyze(Arrays.asList(weak, strong));

        assertTrue(report.isWeak(weak.getId()));
        assertFalse(report.isWeak(strong.getId()));
        assertEquals(1, report.getWeakCount());
    }

    @Test
    void findsReusedPasswords() {
        Credential a = new Credential("gmail.com", "u", "Same#Pass2026", null, null);
        Credential b = new Credential("twitter.com", "u", "Same#Pass2026", null, null);
        Credential c = new Credential("github.com", "u", "Other#Pass2026", null, null);
        SecurityReport report = analyzer.analyze(Arrays.asList(a, b, c));

        assertTrue(report.isReused(a.getId()));
        assertTrue(report.isReused(b.getId()));
        assertFalse(report.isReused(c.getId()));
        assertEquals(2, report.getReusedCount());
    }

    @Test
    void attentionCountDoesNotDoubleCount() {
        Credential a = new Credential("a.com", "u", "123456", null, null);   // weak and reused
        Credential b = new Credential("b.com", "u", "123456", null, null);
        SecurityReport report = analyzer.analyze(Arrays.asList(a, b));
        assertEquals(2, report.getWeakCount());
        assertEquals(2, report.getReusedCount());
        assertEquals(2, report.getAttentionCount());
        assertTrue(report.needsAttention(a.getId()));
    }

    @Test
    void summaryIsReadable() {
        Credential weak = new Credential("a.com", "u", "sunshine", null, null);
        Credential r1 = new Credential("b.com", "u", "Same#Pass2026", null, null);
        Credential r2 = new Credential("c.com", "u", "Same#Pass2026", null, null);
        assertEquals("1 weak password and 2 reused passwords",
                analyzer.analyze(Arrays.asList(weak, r1, r2)).getSummary());
        assertEquals("All your passwords look healthy",
                analyzer.analyze(Collections.singletonList(
                        new Credential("x.com", "u", "Very#Strong2026!", null, null))).getSummary());
    }
}
