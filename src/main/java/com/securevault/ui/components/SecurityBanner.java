package com.securevault.ui.components;

import com.securevault.service.SecurityReport;
import com.securevault.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;

/**
 * A one-line message above the list:
 *  - orange when some passwords are weak or reused, with a "Review" button
 *  - green when everything is fine
 */
public class SecurityBanner extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel message = new JLabel();
    private final JButton actionButton = new JButton();
    private boolean reviewing = false;

    public SecurityBanner(Runnable onToggleReview) {
        super(new BorderLayout(10, 0));
        message.setFont(Theme.BODY);
        actionButton.setFocusable(false);
        actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actionButton.addActionListener(e -> onToggleReview.run());
        add(message, BorderLayout.CENTER);
        add(actionButton, BorderLayout.EAST);
    }

    /** Updates the text and colours from a new report. */
    public void update(SecurityReport report, boolean reviewing) {
        this.reviewing = reviewing;
        if (report.getTotal() == 0) {
            setVisible(false);
            return;
        }
        setVisible(true);
        if (report.hasIssues()) {
            style(Theme.WARNING_BG, Theme.WARNING_BORDER, Theme.WARNING);
            message.setText(reviewing
                    ? "Showing " + report.getAttentionCount()
                        + (report.getAttentionCount() == 1 ? " password that needs" : " passwords that need")
                        + " attention"
                    : "⚠  " + report.getSummary() + ". Changing them makes your accounts safer.");
            actionButton.setText(reviewing ? "Show all" : "Review");
            actionButton.setVisible(true);
        } else {
            style(Theme.SUCCESS_BG, Theme.SUCCESS_BORDER, Theme.SUCCESS);
            message.setText("✔  " + report.getSummary());
            actionButton.setVisible(reviewing);
            actionButton.setText("Show all");
        }
    }

    public boolean isReviewing() {
        return reviewing;
    }

    private void style(java.awt.Color bg, java.awt.Color border, java.awt.Color text) {
        setBackground(bg);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border), Theme.padding(8, 12, 8, 8)));
        message.setForeground(text);
        message.setFont(Theme.BODY.deriveFont(Font.BOLD));
    }
}
