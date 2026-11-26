package com.securevault.ui;

import com.securevault.exception.AuthenticationException;
import com.securevault.exception.CryptoException;
import com.securevault.exception.StorageException;
import com.securevault.exception.ValidationException;
import com.securevault.util.AppLogger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;

/**
 * Turns exceptions into friendly messages, logs unexpected ones and shows
 * them in a dialog. Used everywhere so errors are handled the same way.
 */
public final class ErrorHandler {

    private ErrorHandler() {
    }

    /** A message suitable for showing to the user. */
    public static String friendlyMessage(Throwable error) {
        if (error instanceof ValidationException || error instanceof AuthenticationException) {
            return error.getMessage();
        }
        if (error instanceof CryptoException) {
            return "The vault could not be decrypted. The file may be damaged.";
        }
        if (error instanceof StorageException) {
            return "A file problem occurred: " + error.getMessage()
                    + "\nCheck that the data folder exists and is not read-only.";
        }
        if (error instanceof OutOfMemoryError) {
            return "The application ran out of memory.";
        }
        return "Something went wrong: " + (error.getMessage() != null ? error.getMessage()
                : error.getClass().getSimpleName());
    }

    /** True for errors caused by user input - those are not written to the log. */
    public static boolean isExpected(Throwable error) {
        return error instanceof ValidationException || error instanceof AuthenticationException;
    }

    /** Logs (if unexpected) and shows the error in a dialog. */
    public static void show(Component parent, String action, Throwable error) {
        if (!isExpected(error)) {
            AppLogger.error(action + " failed", error);
        }
        Runnable dialog = () -> JOptionPane.showMessageDialog(parent,
                friendlyMessage(error), action + " failed", JOptionPane.ERROR_MESSAGE);
        if (SwingUtilities.isEventDispatchThread()) {
            dialog.run();
        } else {
            SwingUtilities.invokeLater(dialog);
        }
    }

    /** Catches anything nobody else caught, so the app never just disappears. */
    public static void installGlobalHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            AppLogger.error("Uncaught error in thread " + thread.getName(), error);
            show(null, "Operation", error);
        });
    }
}
