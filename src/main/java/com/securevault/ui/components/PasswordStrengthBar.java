package com.securevault.ui.components;

import com.securevault.model.PasswordStrength;
import com.securevault.security.PasswordStrengthChecker;
import com.securevault.ui.Theme;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A coloured bar that shows how strong a password is.
 * Painted by hand so it looks the same with every look and feel.
 */
public class PasswordStrengthBar extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int BAR_HEIGHT = 8;

    private final transient PasswordStrengthChecker checker = new PasswordStrengthChecker();
    private PasswordStrength strength = PasswordStrength.VERY_WEAK;
    private boolean empty = true;

    public PasswordStrengthBar() {
        setFont(Theme.SMALL);
    }

    /** Re-rates the given password and repaints. */
    public void update(String password) {
        empty = password == null || password.isEmpty();
        strength = checker.evaluate(password);
        repaint();
    }

    public PasswordStrength getStrength() {
        return strength;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(200, BAR_HEIGHT + fm.getHeight() + 4);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = getWidth();

        g2.setColor(Theme.BORDER);
        g2.fillRoundRect(0, 0, width, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);

        if (!empty) {
            int filled = Math.max(BAR_HEIGHT, width * strength.getScore() / 100);
            g2.setColor(strength.getColor());
            g2.fillRoundRect(0, 0, filled, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(empty ? Theme.MUTED : strength.getColor());
        String text = empty ? "Password strength" : "Strength: " + strength.getLabel();
        g2.drawString(text, 0, BAR_HEIGHT + 2 + fm.getAscent());
        g2.dispose();
    }
}
