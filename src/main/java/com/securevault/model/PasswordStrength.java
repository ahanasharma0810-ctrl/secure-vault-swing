package com.securevault.model;

import java.awt.Color;

/** How strong a password is. Each level has a label, a score (0-100) and a colour for the UI. */
public enum PasswordStrength {

    VERY_WEAK("Very weak", 10, new Color(198, 40, 40)),
    WEAK("Weak", 30, new Color(239, 108, 0)),
    MEDIUM("Medium", 55, new Color(249, 168, 37)),
    STRONG("Strong", 80, new Color(124, 179, 66)),
    VERY_STRONG("Very strong", 100, new Color(46, 125, 50));

    private final String label;
    private final int score;
    private final Color color;

    PasswordStrength(String label, int score, Color color) {
        this.label = label;
        this.score = score;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public int getScore() {
        return score;
    }

    public Color getColor() {
        return color;
    }
}
