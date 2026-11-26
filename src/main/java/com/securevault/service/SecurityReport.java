package com.securevault.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The result of checking the vault: which entries have weak or reused passwords.
 * Immutable - create a new report after the vault changes.
 */
public final class SecurityReport {

    private final int total;
    private final Set<String> weakIds;
    private final Set<String> reusedIds;

    public SecurityReport(int total, Set<String> weakIds, Set<String> reusedIds) {
        this.total = total;
        this.weakIds = Collections.unmodifiableSet(new HashSet<>(weakIds));
        this.reusedIds = Collections.unmodifiableSet(new HashSet<>(reusedIds));
    }

    public int getTotal() {
        return total;
    }

    public int getWeakCount() {
        return weakIds.size();
    }

    public int getReusedCount() {
        return reusedIds.size();
    }

    public boolean isWeak(String credentialId) {
        return weakIds.contains(credentialId);
    }

    public boolean isReused(String credentialId) {
        return reusedIds.contains(credentialId);
    }

    /** True if the entry is weak or reused. */
    public boolean needsAttention(String credentialId) {
        return isWeak(credentialId) || isReused(credentialId);
    }

    /** Number of different entries that are weak, reused or both. */
    public int getAttentionCount() {
        Set<String> all = new HashSet<>(weakIds);
        all.addAll(reusedIds);
        return all.size();
    }

    public boolean hasIssues() {
        return !weakIds.isEmpty() || !reusedIds.isEmpty();
    }

    /** A short sentence for the dashboard, e.g. "2 weak passwords and 1 reused password". */
    public String getSummary() {
        if (!hasIssues()) {
            return "All your passwords look healthy";
        }
        StringBuilder sb = new StringBuilder();
        if (!weakIds.isEmpty()) {
            sb.append(plural(weakIds.size(), "weak password"));
        }
        if (!reusedIds.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" and ");
            }
            sb.append(plural(reusedIds.size(), "reused password"));
        }
        return sb.toString();
    }

    private static String plural(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }
}
