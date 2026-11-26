package com.securevault.ui;

import com.securevault.config.AppConfig;
import com.securevault.exception.AccountLockedException;
import com.securevault.exception.AuthenticationException;
import com.securevault.service.Session;
import com.securevault.service.VaultService;
import com.securevault.ui.components.PasswordInput;
import com.securevault.ui.components.SiteAvatar;
import com.securevault.ui.components.WelcomePanel;
import com.securevault.util.AppLogger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

/**
 * Login screen.
 *
 *  Left:  blue welcome panel with the logo and why the app is safe.
 *  Right: "Welcome back, name", the master password box, a Caps Lock warning,
 *         the Unlock button and a progress bar while unlocking.
 *
 * There is one account per computer, so the username is already known and
 * the user only types the master password.
 */
public class LoginFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;
    private static final int FORM_WIDTH = 340;

    private final String savedUsername;
    private final JTextField usernameField = new JTextField(22);   // only used if the name is unknown
    private final PasswordInput passwordInput = new PasswordInput(22);
    private final JButton unlockButton = Theme.primaryButton("Unlock Vault");
    private final JLabel capsLockLabel = Theme.label(" ", Theme.SMALL, Theme.WARNING);
    private final JLabel messageLabel = Theme.label(" ", Theme.SMALL, Theme.DANGER);
    private final JProgressBar progressBar = new JProgressBar();
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
        this.savedUsername = loadSavedUsername();
        buildUI();
        installCapsLockWatch();
        finishSetup(new Dimension(860, 540));
        setResizable(false);
        SwingUtilities.invokeLater(() -> {
            if (savedUsername == null) {
                usernameField.requestFocusInWindow();
            } else {
                passwordInput.requestFocusInWindow();
            }
        });
    }

    private String loadSavedUsername() {
        try {
            return controller.getStorage().loadUser().getUsername();
        } catch (Exception e) {
            return null;   // account file unreadable - ask for the name instead
        }
    }

    // =====================================================================
    // Layout
    // =====================================================================

    private void buildUI() {
        setLayout(new BorderLayout());
        add(new WelcomePanel(AppConfig.APP_NAME, "Your passwords, safe in one place.",
                "AES-256 encryption",
                "Stored only on this computer",
                "Locks itself when you are away"), BorderLayout.WEST);

        JPanel right = new JPanel(new GridBagLayout());   // centres the form
        right.setBackground(Theme.SURFACE);
        right.add(buildForm());
        add(right, BorderLayout.CENTER);

        getRootPane().setDefaultButton(unlockButton);   // Enter = Unlock
    }

    private JComponent buildForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(left(buildGreeting()));
        form.add(Box.createVerticalStrut(30));

        if (savedUsername == null) {
            form.add(left(fieldLabel("Username")));
            form.add(Box.createVerticalStrut(6));
            usernameField.setFont(Theme.BODY.deriveFont(14f));
            form.add(left(fixedHeight(usernameField, 38)));
            form.add(Box.createVerticalStrut(16));
        }

        form.add(left(fieldLabel("Master password")));
        form.add(Box.createVerticalStrut(6));
        passwordInput.getField().setFont(Theme.BODY.deriveFont(15f));
        form.add(left(fixedHeight(passwordInput, 40)));
        form.add(Box.createVerticalStrut(6));
        form.add(left(capsLockLabel));
        form.add(Box.createVerticalStrut(2));
        form.add(left(messageLabel));
        form.add(Box.createVerticalStrut(14));

        unlockButton.setFont(Theme.BODY.deriveFont(Font.BOLD, 15f));
        unlockButton.addActionListener(e -> attemptUnlock());
        form.add(left(fixedHeight(unlockButton, 44)));
        form.add(Box.createVerticalStrut(12));

        progressBar.setStringPainted(true);
        progressBar.setFont(Theme.SMALL);
        progressBar.setVisible(false);
        // the holder always takes up the space, so nothing moves when the bar appears
        JPanel progressHolder = new JPanel(new BorderLayout());
        progressHolder.setOpaque(false);
        progressHolder.add(progressBar, BorderLayout.CENTER);
        form.add(left(fixedHeight(progressHolder, 22)));
        form.add(Box.createVerticalStrut(24));

        JLabel footnote = Theme.label("<html>Forgot your master password? It cannot be recovered "
                + "- that is what keeps your data safe.</html>", Theme.SMALL.deriveFont(Font.ITALIC), Theme.MUTED);
        form.add(left(footnote));

        form.setPreferredSize(new Dimension(FORM_WIDTH, form.getPreferredSize().height));
        return form;
    }

    private JComponent buildGreeting() {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        String name = savedUsername != null ? savedUsername : "?";
        row.add(new SiteAvatar(name, 54), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.label("Welcome back" + (savedUsername != null ? "," : ""), Theme.BODY.deriveFont(15f),
                Theme.MUTED));
        text.add(Theme.label(savedUsername != null ? savedUsername : "Unlock your vault",
                Theme.TITLE.deriveFont(24f), Theme.TEXT));
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private static JLabel fieldLabel(String text) {
        return Theme.label(text, Theme.BODY.deriveFont(Font.BOLD, 14f), Theme.TEXT);
    }

    /** Stretches the component to the form width with a fixed height. */
    private static JComponent fixedHeight(JComponent c, int height) {
        c.setPreferredSize(new Dimension(FORM_WIDTH, height));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return c;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    // =====================================================================
    // Caps Lock warning
    // =====================================================================

    private void installCapsLockWatch() {
        passwordInput.getField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                updateCapsLock();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                updateCapsLock();
            }
        });
        passwordInput.getField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateCapsLock();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                updateCapsLock();
            }
        });
    }

    private void updateCapsLock() {
        boolean on;
        try {
            on = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (UnsupportedOperationException e) {
            on = false;   // some systems cannot report it
        }
        capsLockLabel.setText(on ? "⚠  Caps Lock is on" : " ");
    }

    // =====================================================================
    // Unlocking
    // =====================================================================

    private void attemptUnlock() {
        String username = savedUsername != null ? savedUsername : usernameField.getText();
        char[] password = passwordInput.getPassword();
        setFormEnabled(false);
        showMessage(" ", Theme.DANGER);
        showProgress("Unlocking...");
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
        progressBar.setVisible(false);
        passwordInput.setText("");
        ShakeAnimator.shake(this);
        if (error instanceof AccountLockedException) {
            startLockout(((AccountLockedException) error).getSecondsRemaining());
            return;
        }
        setFormEnabled(true);
        if (!ErrorHandler.isExpected(error)) {
            AppLogger.error("Unlock failed", error);
        }
        showMessage("<html>" + messageFor(error) + "</html>", Theme.DANGER);
        passwordInput.requestFocusInWindow();
    }

    /** Wording for the error. The username is not typed here, so only the password can be wrong. */
    private String messageFor(Throwable error) {
        if (error instanceof AuthenticationException && savedUsername != null) {
            int left = ((AuthenticationException) error).getAttemptsRemaining();
            return "Wrong master password. " + left + (left == 1 ? " attempt" : " attempts") + " left.";
        }
        return ErrorHandler.friendlyMessage(error);
    }

    /** Disables the form and shows a bar that empties until the user may try again. */
    private void startLockout(long seconds) {
        setFormEnabled(false);
        showMessage("Too many wrong attempts. Please wait.", Theme.DANGER);
        progressBar.setIndeterminate(false);
        progressBar.setMaximum((int) seconds);
        progressBar.setValue((int) seconds);
        progressBar.setString("Try again in " + seconds + "s");
        progressBar.setVisible(true);

        final long[] remaining = {seconds};
        if (lockoutTimer != null) {
            lockoutTimer.stop();
        }
        lockoutTimer = new Timer(1000, e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                ((Timer) e.getSource()).stop();
                progressBar.setVisible(false);
                setFormEnabled(true);
                showMessage(" ", Theme.DANGER);
                passwordInput.requestFocusInWindow();
            } else {
                progressBar.setValue((int) remaining[0]);
                progressBar.setString("Try again in " + remaining[0] + "s");
            }
        });
        lockoutTimer.start();
    }

    private void showProgress(String text) {
        progressBar.setIndeterminate(true);
        progressBar.setString(text);
        progressBar.setVisible(true);
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        unlockButton.setEnabled(enabled);
    }

    private void showMessage(String text, Color color) {
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
