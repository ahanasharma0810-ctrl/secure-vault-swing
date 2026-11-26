package com.securevault.util;

import com.securevault.config.AppConfig;

import javax.swing.Timer;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

/**
 * Copies text to the clipboard and wipes it again after a few seconds,
 * so a copied password does not stay there for other programs to read.
 * If the user copies something else in the meantime, it is left alone.
 */
public class ClipboardManager {

    private final Clipboard clipboard;
    private final Timer clearTimer;
    private String lastCopied;

    public ClipboardManager() {
        this(Toolkit.getDefaultToolkit().getSystemClipboard(), AppConfig.CLIPBOARD_CLEAR_SECONDS);
    }

    public ClipboardManager(Clipboard clipboard, int clearAfterSeconds) {
        this.clipboard = clipboard;
        this.clearTimer = new Timer(clearAfterSeconds * 1000, e -> clearIfUnchanged());
        this.clearTimer.setRepeats(false);
    }

    /** Copies the text and starts the countdown to clear it. */
    public void copy(String text) {
        clipboard.setContents(new StringSelection(text), null);
        lastCopied = text;
        clearTimer.restart();
    }

    /** Clears the clipboard, but only if it still holds what we copied. */
    public void clearIfUnchanged() {
        clearTimer.stop();
        if (lastCopied != null && lastCopied.equals(currentText())) {
            clipboard.setContents(new StringSelection(""), null);
        }
        lastCopied = null;
    }

    public boolean isClearPending() {
        return clearTimer.isRunning();
    }

    private String currentText() {
        try {
            return (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (Exception e) {
            return null;   // clipboard busy or holds something that is not text
        }
    }
}
