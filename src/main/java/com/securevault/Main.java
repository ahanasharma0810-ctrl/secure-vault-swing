package com.securevault;

import com.securevault.config.AppConfig;

/**
 * Entry point of the application.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println(AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION);
        System.out.println("Data folder: " + AppConfig.DATA_FOLDER);
    }
}
