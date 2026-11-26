package com.securevault.ui;

import com.securevault.service.AuthService;
import com.securevault.service.Session;
import com.securevault.service.VaultService;
import com.securevault.storage.StorageService;

import javax.swing.JFrame;

/**
 * Decides which window is on screen (register, login or dashboard) and
 * moves between them. Windows never create each other directly - they ask
 * the controller, which keeps them independent of one another.
 */
public class AppController {

    private final StorageService storage;
    private final AuthService authService;
    private JFrame currentWindow;

    public AppController(StorageService storage) {
        this.storage = storage;
        this.authService = new AuthService(storage);   // one instance so the lock-out counter is kept
    }

    public StorageService getStorage() {
        return storage;
    }

    public AuthService getAuthService() {
        return authService;
    }

    /** First screen: register if there is no account yet, otherwise log in. */
    public void start() {
        if (authService.isRegistered()) {
            showLogin();
        } else {
            showRegister();
        }
    }

    public void showRegister() {
        switchTo(new RegisterFrame(this));
    }

    public void showLogin() {
        switchTo(new LoginFrame(this));
    }

    /** Called after a successful login or registration. */
    public void showDashboard(Session session, VaultService vault) {
        switchTo(new DashboardFrame(this, session, vault));
    }

    /** Locks the vault: the dashboard (and the key in memory) is thrown away. */
    public void lock() {
        showLogin();
    }

    public void exit() {
        if (currentWindow != null) {
            currentWindow.dispose();
        }
        System.exit(0);
    }

    private void switchTo(JFrame next) {
        JFrame previous = currentWindow;
        currentWindow = next;
        next.setVisible(true);
        if (previous != null) {
            previous.dispose();
        }
    }
}
