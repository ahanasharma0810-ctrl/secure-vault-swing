package com.securevault.service;

import com.securevault.model.Credential;
import com.securevault.model.PasswordStrength;
import com.securevault.security.PasswordStrengthChecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Looks through all credentials and finds the ones that need attention:
 *  - weak passwords (rated "Very weak" or "Weak")
 *  - passwords used for more than one entry
 */
public class SecurityAnalyzer {

    private final PasswordStrengthChecker checker = new PasswordStrengthChecker();

    public SecurityReport analyze(List<Credential> credentials) {
        Set<String> weak = new HashSet<>();
        Map<String, Set<String>> idsByPassword = new HashMap<>();

        for (Credential c : credentials) {
            if (checker.evaluate(c.getPassword()).getScore() < PasswordStrength.MEDIUM.getScore()) {
                weak.add(c.getId());
            }
            idsByPassword.computeIfAbsent(c.getPassword(), k -> new HashSet<>()).add(c.getId());
        }

        Set<String> reused = new HashSet<>();
        for (Set<String> ids : idsByPassword.values()) {
            if (ids.size() > 1) {
                reused.addAll(ids);
            }
        }
        return new SecurityReport(credentials.size(), weak, reused);
    }

    /** How strong one password is (used to colour the dot on each card). */
    public PasswordStrength strengthOf(String password) {
        return checker.evaluate(password);
    }
}
