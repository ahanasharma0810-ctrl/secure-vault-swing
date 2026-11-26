package com.securevault.util;

import com.securevault.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Writes errors to securevault.log in the data folder so problems can be
 * investigated later. Passwords are never passed to the logger.
 */
public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("com.securevault");
    private static boolean initialised = false;

    private AppLogger() {
    }

    private static synchronized void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        try {
            File folder = new File(AppConfig.DATA_FOLDER);
            if (folder.isDirectory() || folder.mkdirs()) {
                FileHandler handler = new FileHandler(
                        new File(folder, "securevault.log").getPath(), 512 * 1024, 1, true);
                handler.setFormatter(new SimpleFormatter());
                LOGGER.addHandler(handler);
            }
        } catch (IOException | SecurityException e) {
            // logging to the console only is fine
        }
    }

    public static void info(String message) {
        init();
        LOGGER.info(message);
    }

    public static void warn(String message) {
        init();
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable error) {
        init();
        LOGGER.log(Level.SEVERE, message, error);
    }
}
