package com.securevault;

import com.securevault.storage.FileStorageService;
import com.securevault.ui.AppController;
import com.securevault.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * Entry point: applies the theme and opens the first window.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new AppController(new FileStorageService()).start();
        });
    }
}
