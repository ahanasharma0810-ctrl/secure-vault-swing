package com.securevault.ui;

import com.securevault.config.AppConfig;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Parent of every main window. Gives all windows the same title format, icon,
 * background and the common helper methods (error/info/confirm dialogs).
 */
public abstract class BaseFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    protected final AppController controller;

    protected BaseFrame(AppController controller, String subtitle) {
        super(AppConfig.APP_NAME + " - " + subtitle);
        this.controller = controller;
        setIconImages(Theme.appIcons());
        getContentPane().setBackground(Theme.BACKGROUND);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseRequested();
            }
        });
    }

    /** Subclasses call this at the end of their constructor. */
    protected void finishSetup(Dimension minimumSize) {
        pack();
        setMinimumSize(minimumSize);
        if (getWidth() < minimumSize.width || getHeight() < minimumSize.height) {
            setSize(Math.max(getWidth(), minimumSize.width), Math.max(getHeight(), minimumSize.height));
        }
        setLocationRelativeTo(null);
    }

    /** What happens when the user clicks the window's X button. Default: quit the app. */
    protected void onCloseRequested() {
        controller.exit();
    }

    /** Shows a wait cursor while slow work (like hashing) runs. */
    protected void setBusy(boolean busy) {
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, AppConfig.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
