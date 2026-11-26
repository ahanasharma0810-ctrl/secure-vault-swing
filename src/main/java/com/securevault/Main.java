package com.securevault;

import com.securevault.config.AppConfig;
import com.securevault.storage.FileStorageService;
import com.securevault.ui.AppController;
import com.securevault.ui.ErrorHandler;
import com.securevault.ui.Theme;
import com.securevault.util.AppLogger;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Entry point: applies the theme and opens the first window.
 */
public class Main {

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(AppConfig.APP_NAME + " needs a desktop with a screen to run.");
            System.exit(1);
        }
        ErrorHandler.installGlobalHandler();
        AppLogger.info(AppConfig.APP_NAME + " " + AppConfig.APP_VERSION + " starting");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new AppController(new FileStorageService()).start();
        });
    }
}
