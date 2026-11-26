package com.securevault.config;

import java.io.File;

/**
 * Central place for every fixed setting of the application.
 * Other classes read values from here instead of hard-coding numbers.
 */
public final class AppConfig {

    private AppConfig() {
        // utility class - no objects
    }

    // ---------- General ----------
    public static final String APP_NAME = "Secure Vault";
    public static final String APP_VERSION = "1.0.0";

    // ---------- Files ----------
    /** Can be overridden with -Dsecurevault.home=some/folder (used by tests). */
    public static final String DATA_FOLDER = System.getProperty(
            "securevault.home",
            System.getProperty("user.home") + File.separator + ".securevault");
    public static final String USER_FILE_NAME = "user.properties";
    public static final String VAULT_FILE_NAME = "vault.dat";

    // ---------- Master password rules ----------
    public static final int MIN_MASTER_PASSWORD_LENGTH = 8;
    public static final int MAX_LOGIN_ATTEMPTS = 3;
    public static final int LOCKOUT_SECONDS = 30;

    // ---------- Security ----------
    public static final int PBKDF2_ITERATIONS = 210_000;
    public static final int KEY_LENGTH_BITS = 256;
    public static final int SALT_LENGTH_BYTES = 16;
    public static final int GCM_IV_LENGTH_BYTES = 12;
    public static final int GCM_TAG_LENGTH_BITS = 128;

    // ---------- Password generator ----------
    public static final int DEFAULT_GENERATED_LENGTH = 16;
    public static final int MIN_GENERATED_LENGTH = 6;
    public static final int MAX_GENERATED_LENGTH = 64;

    // ---------- Usability ----------
    public static final int CLIPBOARD_CLEAR_SECONDS = 15;
    public static final int AUTO_LOCK_MINUTES = 5;

    // ---------- Categories shown in the drop-down ----------
    public static final String[] DEFAULT_CATEGORIES = {
            "General", "Email", "Social", "Banking", "Shopping", "Work", "Education", "Entertainment"
    };
}
