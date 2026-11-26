package com.securevault.ui.components;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * A password field with a Show/Hide button next to it.
 */
public class PasswordInput extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPasswordField field;
    private final JToggleButton toggle = new JToggleButton("Show");
    private final char echoChar;

    public PasswordInput(int columns) {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        field = new JPasswordField(columns);
        echoChar = field.getEchoChar();

        toggle.setFocusable(false);
        toggle.setPreferredSize(new Dimension(70, toggle.getPreferredSize().height));
        toggle.addActionListener(e -> setRevealed(toggle.isSelected()));

        add(field, BorderLayout.CENTER);
        add(toggle, BorderLayout.EAST);
    }

    public void setRevealed(boolean revealed) {
        toggle.setSelected(revealed);
        field.setEchoChar(revealed ? (char) 0 : echoChar);
        toggle.setText(revealed ? "Hide" : "Show");
    }

    /** Returns a copy of the password. Callers should wipe it with Arrays.fill when done. */
    public char[] getPassword() {
        return field.getPassword();
    }

    public String getText() {
        return new String(field.getPassword());
    }

    public void setText(String text) {
        field.setText(text);
    }

    public boolean isEmpty() {
        return field.getDocument().getLength() == 0;
    }

    public JPasswordField getField() {
        return field;
    }

    /** Runs the given code every time the text changes. */
    public void onChange(Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
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
    public boolean requestFocusInWindow() {
        return field.requestFocusInWindow();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
        toggle.setEnabled(enabled);
    }
}
