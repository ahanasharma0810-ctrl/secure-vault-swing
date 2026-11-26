package com.securevault.ui.components;

import com.securevault.ui.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A coloured circle with the first letter of a website, like an app icon.
 * The same website always gets the same colour.
 */
public class SiteAvatar extends JComponent {

    private static final long serialVersionUID = 1L;

    private final String letter;
    private final Color color;
    private final int size;

    public SiteAvatar(String website, int size) {
        this.letter = initialFor(website);
        this.color = colorFor(website);
        this.size = size;
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size * 0.45f)));
    }

    /** First letter or digit of the name, in capitals ("?" if there is none). */
    public static String initialFor(String website) {
        if (website != null) {
            for (char c : website.toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    return String.valueOf(Character.toUpperCase(c));
                }
            }
        }
        return "?";
    }

    /** Picks a colour from the palette based on the name, so it never changes. */
    public static Color colorFor(String website) {
        String key = website == null ? "" : website.toLowerCase();
        int index = Math.floorMod(key.hashCode(), Theme.AVATAR_COLORS.length);
        return Theme.AVATAR_COLORS[index];
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g2.setColor(color);
        g2.fillOval(x, y, size, size);

        g2.setColor(Color.WHITE);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (size - fm.stringWidth(letter)) / 2;
        int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(letter, tx, ty);
        g2.dispose();
    }
}
