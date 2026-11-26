package com.securevault.ui;

import com.securevault.config.AppConfig;
import com.securevault.exception.AccountLockedException;
import com.securevault.service.Session;
import com.securevault.service.VaultService;
import com.securevault.ui.components.FormPanel;
import com.securevault.ui.components.HeaderPanel;
import com.securevault.ui.components.PasswordInput;
import com.securevault.util.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;

/**
 * Login screen: asks for the username and master password and unlocks the vault.
 */
public class LoginFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField usernameField = new JTextField(22);
    private final PasswordInput passwordInput = new PasswordInput(22);
    private final JButton unlockButton = Theme.primaryButton("Unlock Vault");
    private final JLabel messageLabel = Theme.label(" ", Theme.SMALL, Theme.DANGER);
    private Timer lockoutTimer;

    /** Result of a successful unlock: the session plus the decrypted vault. */
    private static final class Unlocked {
        final Session session;
        final VaultService vault;

        Unlocked(Session session, VaultService vault) {
            this.session = session;
            this.vault = vault;
        }
    }

    public LoginFrame(AppController controller) {
        super(controller, "Unlock");
        buildUI();
        prefillUsername();
        finishSetup(new Dimension(440, 470));
        setResizable(false);
        SwingUtilities.invokeLater(() -> {
            if (usernameField.getText().isEmpty()) {
                usernameField.requestFocusInWindow();
            } else {
                passwordInput.requestFocusInWindow();
            }
        });
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(new HeaderPanel(AppConfig.APP_NAME, "Your passwords, locked and safe"), BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), Theme.padding(24, 28, 24, 28)));

        JPanel intro = new JPanel(new BorderLayout(0, 4));
        intro.setOpaque(false);
        intro.add(Theme.label("Welcome back", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        intro.add(Theme.label("Enter your master password to unlock the vault.", Theme.SMALL, Theme.MUTED),
                BorderLayout.CENTER);
        card.add(intro, BorderLayout.NORTH);

        FormPanel form = new FormPanel();
        form.addField("Username", usernameField);
        form.addField("Master password", passwordInput);
        form.addRow(messageLabel, 8);
        JPanel formHolder = new JPanel(new BorderLayout());   // keeps the form at the top
        formHolder.setOpaque(false);
        formHolder.add(form, BorderLayout.NORTH);
        card.add(formHolder, BorderLayout.CENTER);

        unlockButton.setPreferredSize(new Dimension(10, 38));
        unlockButton.addActionListener(e -> attemptUnlock());
        card.add(unlockButton, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(Theme.padding(18));
        wrapper.add(card, BorderLayout.CENTER);

        JLabel footnote = Theme.label("<html><center>A forgotten master password cannot be recovered.<br>"
                + "That is what keeps your data safe.</center></html>", Theme.SMALL, Theme.MUTED);
        footnote.setHorizontalAlignment(JLabel.CENTER);
        footnote.setFont(Theme.SMALL.deriveFont(Font.ITALIC));
        wrapper.add(footnote, BorderLayout.SOUTH);
        add(wrapper, BorderLayout.CENTER);

        getRootPane().setDefaultButton(unlockButton);   // Enter key = Unlock
    }

    private void prefillUsername() {
        try {
            usernameField.setText(controller.getStorage().loadUser().getUsername());
        } catch (Exception e) {
            // leave it empty - the user can still type it
        }
    }

    private void attemptUnlock() {
        String username = usernameField.getText();
        char[] password = passwordInput.getPassword();
        setFormEnabled(false);
        showMessage("Unlocking...", Theme.MUTED);
        setBusy(true);

        BackgroundTask.run(() -> {
            Session session = controller.getAuthService().login(username, password);
            VaultService vault = new VaultService(controller.getStorage(), session.getEncryptionService());
            vault.load();
            return new Unlocked(session, vault);
        }, unlocked -> {
            Arrays.fill(password, '\0');
            setBusy(false);
            controller.showDashboard(unlocked.session, unlocked.vault);
        }, error -> {
            Arrays.fill(password, '\0');
            setBusy(false);
            handleError(error);
        });
    }

    private void handleError(Throwable error) {
        passwordInput.setText("");
        if (error instanceof AccountLockedException) {
            startLockout(((AccountLockedException) error).getSecondsRemaining());
            return;
        }
        setFormEnabled(true);
        if (!ErrorHandler.isExpected(error)) {
            AppLogger.error("Unlock failed", error);
        }
        showMessage("<html>" + ErrorHandler.friendlyMessage(error) + "</html>", Theme.DANGER);
        passwordInput.requestFocusInWindow();
    }

    /** Disables the form and counts down until the user may try again. */
    private void startLockout(long seconds) {
        setFormEnabled(false);
        final long[] remaining = {seconds};
        showMessage("Too many failed attempts. Try again in " + remaining[0] + "s", Theme.DANGER);
        if (lockoutTimer != null) {
            lockoutTimer.stop();
        }
        lockoutTimer = new Timer(1000, e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                ((Timer) e.getSource()).stop();
                setFormEnabled(true);
                showMessage(" ", Theme.DANGER);
                passwordInput.requestFocusInWindow();
            } else {
                showMessage("Too many failed attempts. Try again in " + remaining[0] + "s", Theme.DANGER);
            }
        });
        lockoutTimer.start();
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        unlockButton.setEnabled(enabled);
    }

    private void showMessage(String text, java.awt.Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    @Override
    public void dispose() {
        if (lockoutTimer != null) {
            lockoutTimer.stop();
        }
        super.dispose();
    }
}
