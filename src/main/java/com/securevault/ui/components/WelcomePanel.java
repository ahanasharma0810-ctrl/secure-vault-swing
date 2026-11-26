package com.securevault.ui.components;

import com.securevault.ui.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * The blue welcome side of the start screens: logo, app name, a tagline
 * and a few short points that tell the user why the app is safe.
 */
public class WelcomePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color SOFT_WHITE = new Color(219, 234, 254);

    public WelcomePanel(String title, String tagline, String... points) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Theme.padding(48, 40, 40, 36));

        JLabel logo = new JLabel(new ImageIcon(Theme.lockIcon(76)));
        add(logo);
        add(Box.createVerticalStrut(22));

        add(Theme.label(title, Theme.TITLE.deriveFont(28f), Color.WHITE));
        add(Box.createVerticalStrut(8));
        add(Theme.label("<html>" + tagline + "</html>", Theme.BODY.deriveFont(15f), SOFT_WHITE));
        add(Box.createVerticalStrut(34));

        for (String point : points) {
            JLabel line = Theme.label("✔   " + point, Theme.BODY.deriveFont(Font.BOLD, 14f), Color.WHITE);
            line.setBorder(Theme.padding(0, 0, 14, 0));
            add(line);
        }
        add(Box.createVerticalGlue());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(340, super.getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), Theme.PRIMARY_DARK));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // two soft circles for decoration
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillOval(getWidth() - 160, getHeight() - 170, 260, 260);
        g2.fillOval(-90, getHeight() - 110, 180, 180);
        g2.dispose();
    }
}
