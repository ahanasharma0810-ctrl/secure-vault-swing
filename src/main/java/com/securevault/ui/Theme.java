package com.securevault.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Colours, fonts and small factory methods so every window looks the same.
 */
public final class Theme {

    private Theme() {
    }

    // ---------- Colours ----------
    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color PRIMARY_DARK = new Color(30, 64, 175);
    public static final Color BACKGROUND = new Color(243, 246, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(31, 41, 55);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color BORDER = new Color(209, 213, 219);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color ROW_ALT = new Color(248, 250, 252);
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255);
    public static final Color HOVER = new Color(248, 250, 252);
    public static final Color WARNING = new Color(180, 83, 9);
    public static final Color WARNING_BG = new Color(255, 247, 237);
    public static final Color WARNING_BORDER = new Color(253, 186, 116);
    public static final Color SUCCESS_BG = new Color(240, 253, 244);
    public static final Color SUCCESS_BORDER = new Color(134, 239, 172);
    public static final Color CHIP_BG = new Color(241, 245, 249);

    /** Colours for the letter icons of websites. */
    public static final Color[] AVATAR_COLORS = {
            new Color(37, 99, 235), new Color(22, 163, 74), new Color(219, 39, 119),
            new Color(234, 88, 12), new Color(124, 58, 237), new Color(13, 148, 136),
            new Color(202, 138, 4), new Color(225, 29, 72), new Color(71, 85, 105)
    };

    // ---------- Fonts ----------
    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    public static final Font CARD_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 15);

    /** Applies the Nimbus look and feel with our colours. Call once at start-up. */
    public static void install() {
        UIManager.put("nimbusBase", PRIMARY_DARK);
        UIManager.put("nimbusFocus", PRIMARY);
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("nimbusOrange", PRIMARY);   // progress bars in our blue
        UIManager.put("control", BACKGROUND);
        UIManager.put("defaultFont", BODY);
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // the default look and feel is fine too
        }
    }

    // ---------- Factory methods ----------

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(DANGER);
        button.setForeground(Color.WHITE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static Border padding(int size) {
        return BorderFactory.createEmptyBorder(size, size, size, size);
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    // ---------- App icon (drawn in code, no image files needed) ----------

    public static List<Image> appIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 32, 48, 64, 128}) {
            icons.add(lockIcon(size));
        }
        return icons;
    }

    public static BufferedImage lockIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float s = size;

        g.setColor(PRIMARY);
        g.fill(new Ellipse2D.Float(0, 0, s, s));

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(s * 0.08f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(s * 0.33f, s * 0.2f, s * 0.34f, s * 0.36f, 0, 180, Arc2D.OPEN));
        g.drawLine(Math.round(s * 0.33f), Math.round(s * 0.38f), Math.round(s * 0.33f), Math.round(s * 0.46f));
        g.drawLine(Math.round(s * 0.67f), Math.round(s * 0.38f), Math.round(s * 0.67f), Math.round(s * 0.46f));
        g.fill(new RoundRectangle2D.Float(s * 0.25f, s * 0.44f, s * 0.5f, s * 0.36f, s * 0.1f, s * 0.1f));

        g.setColor(PRIMARY);
        g.fill(new Ellipse2D.Float(s * 0.46f, s * 0.54f, s * 0.08f, s * 0.08f));
        g.fillRect(Math.round(s * 0.485f), Math.round(s * 0.58f), Math.max(1, Math.round(s * 0.03f)), Math.round(s * 0.1f));
        g.dispose();
        return image;
    }
}
