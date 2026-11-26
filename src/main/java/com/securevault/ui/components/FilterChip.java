package com.securevault.ui.components;

import com.securevault.ui.Theme;

import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A pill-shaped toggle used to filter by category ("All", "Email", ...). */
public class FilterChip extends JToggleButton {

    private static final long serialVersionUID = 1L;

    public FilterChip(String text) {
        super(text);
        setFont(Theme.BODY);
        setFocusable(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(Theme.padding(5, 14, 5, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        boolean on = isSelected();
        boolean hover = getModel().isRollover();

        g2.setColor(on ? Theme.PRIMARY : (hover ? Theme.PRIMARY_LIGHT : Theme.SURFACE));
        g2.fillRoundRect(0, 0, w, h, h, h);
        g2.setColor(on ? Theme.PRIMARY : Theme.BORDER);
        g2.drawRoundRect(0, 0, w, h, h, h);

        g2.setFont(on ? getFont().deriveFont(Font.BOLD) : getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(on ? Color.WHITE : Theme.TEXT);
        g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
