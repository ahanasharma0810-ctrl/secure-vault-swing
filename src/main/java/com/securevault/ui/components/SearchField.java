package com.securevault.ui.components;

import com.securevault.ui.Theme;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/** A text field that shows grey hint text while it is empty. */
public class SearchField extends JTextField {

    private static final long serialVersionUID = 1L;

    private final String hint;

    public SearchField(String hint, int columns) {
        super(columns);
        this.hint = hint;
    }

    /** Runs the given code every time the text changes. */
    public void onChange(Runnable action) {
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.MUTED);
            g2.setFont(getFont().deriveFont(java.awt.Font.ITALIC));
            Insets in = getInsets();
            int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(hint, in.left + 2, y);
            g2.dispose();
        }
    }
}
