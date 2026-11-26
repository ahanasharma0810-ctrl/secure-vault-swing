package com.securevault.model;

import com.securevault.exception.ValidationException;

import java.time.LocalDateTime;

/**
 * One saved login (one row in the dashboard table).
 *
 * While the app is unlocked the password is kept in memory as plain text so it
 * can be shown or copied. On disk the whole vault is encrypted with AES.
 */
public class Credential extends BaseEntity {

    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_CATEGORY = "General";
    private static final int MAX_FIELD_LENGTH = 200;
    private static final int MAX_NOTES_LENGTH = 2000;

    private String website;
    private String username;
    private String password;
    private String category;
    private String notes;

    /** Creates a new credential. */
    public Credential(String website, String username, String password,
                      String category, String notes) {
        super();
        setAll(website, username, password, category, notes);
    }

    /** Re-creates an existing credential with the same id (used when editing). */
    public Credential(String id, LocalDateTime createdAt, String website, String username,
                      String password, String category, String notes) {
        super(id, createdAt);
        setAll(website, username, password, category, notes);
    }

    private void setAll(String website, String username, String password,
                        String category, String notes) {
        setWebsite(website);
        setUsername(username);
        setPassword(password);
        setCategory(category);
        setNotes(notes);
    }

    // ---------- Getters ----------
    public String getWebsite() {
        return website;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCategory() {
        return category;
    }

    public String getNotes() {
        return notes;
    }

    // ---------- Setters with validation ----------
    public final void setWebsite(String website) {
        this.website = limit(requireText(website, "Website"), MAX_FIELD_LENGTH, "Website");
        touch();
    }

    public final void setUsername(String username) {
        this.username = limit(requireText(username, "Username"), MAX_FIELD_LENGTH, "Username");
        touch();
    }

    public final void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password cannot be empty");
        }
        this.password = limit(password, MAX_FIELD_LENGTH, "Password");
        touch();
    }

    public final void setCategory(String category) {
        this.category = (category == null || category.trim().isEmpty())
                ? DEFAULT_CATEGORY
                : limit(category.trim(), MAX_FIELD_LENGTH, "Category");
        touch();
    }

    public final void setNotes(String notes) {
        this.notes = (notes == null) ? "" : limit(notes.trim(), MAX_NOTES_LENGTH, "Notes");
        touch();
    }

    private static String limit(String value, int max, String fieldName) {
        if (value.length() > max) {
            throw new ValidationException(fieldName + " must be at most " + max + " characters");
        }
        return value;
    }

    /** Used by the search box: true if website, username, category or notes contain the keyword. */
    public boolean matches(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String k = keyword.trim().toLowerCase();
        return website.toLowerCase().contains(k)
                || username.toLowerCase().contains(k)
                || category.toLowerCase().contains(k)
                || notes.toLowerCase().contains(k);
    }

    @Override
    public String getDisplayName() {
        return website + " (" + username + ")";
    }

    @Override
    public String toString() {
        // the password is hidden on purpose
        return "Credential{website='" + website + "', username='" + username
                + "', category='" + category + "', password='********'}";
    }
}
