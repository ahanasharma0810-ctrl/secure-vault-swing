package com.securevault.ui.dialogs;

import com.securevault.ui.Theme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Parent of every pop-up dialog: modal, same icon and colours,
 * a button bar at the bottom and the Escape key closes it.
 */
public abstract class BaseDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    protected final JLabel messageLabel = Theme.label(" ", Theme.SMALL, Theme.DANGER);

    protected BaseDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setIconImages(Theme.appIcons());
        getContentPane().setBackground(Theme.BACKGROUND);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Creates the bottom bar: message on the left, buttons on the right. */
    protected JPanel buttonBar(JButton... buttons) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(Theme.padding(10, 0, 0, 0));
        bar.add(messageLabel, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        for (JButton b : buttons) {
            right.add(b);
        }
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    protected void showMessage(String text) {
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText(text == null || text.isEmpty() ? " " : text);
    }

    protected void showNotice(String text) {
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setText(text);
    }

    /** Sizes and centres the dialog over its owner. Subclasses call this last. */
    protected void finishSetup() {
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }
}
