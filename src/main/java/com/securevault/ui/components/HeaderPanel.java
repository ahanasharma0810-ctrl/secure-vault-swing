package com.securevault.ui.components;

import com.securevault.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Blue gradient banner with the app icon, a title and a subtitle. */
public class HeaderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel subtitleLabel;
    private final JPanel rightSide = new JPanel(new BorderLayout());

    public HeaderPanel(String title, String subtitle) {
        super(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel icon = new JLabel(new ImageIcon(Theme.lockIcon(44)));
        add(icon, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.label(title, Theme.TITLE, Color.WHITE));
        subtitleLabel = Theme.label(subtitle, Theme.BODY, new Color(219, 234, 254));
        text.add(subtitleLabel);
        add(text, BorderLayout.CENTER);

        rightSide.setOpaque(false);
        add(rightSide, BorderLayout.EAST);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    /** Puts a component (for example a Lock button) on the right side of the banner. */
    public void setRightComponent(java.awt.Component component) {
        rightSide.removeAll();
        rightSide.add(component, BorderLayout.CENTER);
        rightSide.revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY_DARK, getWidth(), getHeight(), Theme.PRIMARY));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
