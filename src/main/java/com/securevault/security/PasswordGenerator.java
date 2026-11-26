package com.securevault.security;

import com.securevault.config.AppConfig;
import com.securevault.exception.ValidationException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates random passwords with SecureRandom.
 * Each selected character group is guaranteed to appear at least once.
 */
public class PasswordGenerator {

    public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    public static final String DIGITS = "0123456789";
    public static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?/";
    /** Characters that are easy to confuse when reading: O/0, l/1/I. */
    public static final String AMBIGUOUS = "O0Il1";

    private final SecureRandom random = new SecureRandom();

    /** Generates a password using the given options. */
    public String generate(Options options) {
        int length = options.getLength();
        if (length < AppConfig.MIN_GENERATED_LENGTH || length > AppConfig.MAX_GENERATED_LENGTH) {
            throw new ValidationException("Length must be between " + AppConfig.MIN_GENERATED_LENGTH
                    + " and " + AppConfig.MAX_GENERATED_LENGTH);
        }

        List<String> groups = new ArrayList<>();
        if (options.isUppercase()) groups.add(clean(UPPERCASE, options));
        if (options.isLowercase()) groups.add(clean(LOWERCASE, options));
        if (options.isDigits()) groups.add(clean(DIGITS, options));
        if (options.isSymbols()) groups.add(SYMBOLS);
        if (groups.isEmpty()) {
            throw new ValidationException("Select at least one character type");
        }

        List<Character> chars = new ArrayList<>();
        StringBuilder all = new StringBuilder();
        for (String group : groups) {
            chars.add(randomChar(group));   // at least one from each group
            all.append(group);
        }
        while (chars.size() < length) {
            chars.add(randomChar(all.toString()));
        }
        Collections.shuffle(chars, random);

        StringBuilder result = new StringBuilder(length);
        for (char c : chars) {
            result.append(c);
        }
        return result.toString();
    }

    /** Generates a password with the default options. */
    public String generate() {
        return generate(new Options());
    }

    private char randomChar(String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    private static String clean(String group, Options options) {
        if (!options.isExcludeAmbiguous()) {
            return group;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : group.toCharArray()) {
            if (AMBIGUOUS.indexOf(c) < 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Settings for the generator. Setters return 'this' so calls can be chained. */
    public static class Options {
        private int length = AppConfig.DEFAULT_GENERATED_LENGTH;
        private boolean uppercase = true;
        private boolean lowercase = true;
        private boolean digits = true;
        private boolean symbols = true;
        private boolean excludeAmbiguous = false;

        public int getLength() { return length; }
        public boolean isUppercase() { return uppercase; }
        public boolean isLowercase() { return lowercase; }
        public boolean isDigits() { return digits; }
        public boolean isSymbols() { return symbols; }
        public boolean isExcludeAmbiguous() { return excludeAmbiguous; }

        public Options length(int length) { this.length = length; return this; }
        public Options uppercase(boolean on) { this.uppercase = on; return this; }
        public Options lowercase(boolean on) { this.lowercase = on; return this; }
        public Options digits(boolean on) { this.digits = on; return this; }
        public Options symbols(boolean on) { this.symbols = on; return this; }
        public Options excludeAmbiguous(boolean on) { this.excludeAmbiguous = on; return this; }
    }
}
