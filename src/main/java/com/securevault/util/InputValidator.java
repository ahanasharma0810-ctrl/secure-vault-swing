package com.securevault.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Small, reusable checks for text typed by the user.
 * Kept free of Swing so they are easy to unit test.
 */
public final class InputValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern URL_PREFIX = Pattern.compile("^(?i)(https?://)?(www\\.)?");

    private InputValidator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean looksLikeEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    /**
     * Cleans a website so "https://www.Gmail.com/" and "gmail.com" are stored the same way.
     * Text that is not a web address (like "Bank App") is only trimmed.
     */
    public static String normalizeWebsite(String website) {
        if (website == null) {
            return null;
        }
        String trimmed = website.trim();
        if (trimmed.contains(" ") || !trimmed.contains(".")) {
            return trimmed;   // an app name, not a URL
        }
        String clean = URL_PREFIX.matcher(trimmed).replaceFirst("");
        int slash = clean.indexOf('/');
        if (slash >= 0) {
            clean = clean.substring(0, slash);
        }
        return clean.toLowerCase(Locale.ROOT);
    }

    /**
     * Turns a saved website into an address the browser can open,
     * or returns null if it is just an app name (like "Bank App").
     */
    public static String toWebAddress(String website) {
        if (isBlank(website)) {
            return null;
        }
        String w = website.trim();
        if (w.contains(" ") || !w.contains(".")) {
            return null;
        }
        if (w.toLowerCase(Locale.ROOT).startsWith("http://") || w.toLowerCase(Locale.ROOT).startsWith("https://")) {
            return w;
        }
        return "https://" + w;
    }

    /** Removes control characters (e.g. pasted tabs/new lines) from single-line fields. */
    public static String singleLine(String value) {
        return value == null ? null : value.replaceAll("[\\p{Cntrl}]", "").trim();
    }
}
