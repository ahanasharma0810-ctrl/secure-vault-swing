package com.securevault.ui.components;

import com.securevault.ui.Theme;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A small rounded label, e.g. a category name or a "Weak" warning. */
public class Tag extends JLabel {

    private static final long serialVersionUID = 1L;

    private final Color background;

    public Tag(String text, Color foreground, Color background) {
        super(text);
        this.background = background;
        setForeground(foreground);
        setFont(Theme.SMALL);
        setBorder(Theme.padding(2, 8, 2, 8));
        setOpaque(false);
    }

    public static Tag category(String text) {
        return new Tag(text, Theme.MUTED, Theme.CHIP_BG);
    }

    public static Tag warning(String text) {
        return new Tag(text, Theme.WARNING, Theme.WARNING_BG);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
