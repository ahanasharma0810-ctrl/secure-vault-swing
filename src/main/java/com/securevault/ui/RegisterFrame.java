package com.securevault.ui;

import com.securevault.config.AppConfig;
import com.securevault.service.Session;
import com.securevault.service.VaultService;
import com.securevault.ui.components.FormPanel;
import com.securevault.ui.components.HeaderPanel;
import com.securevault.ui.components.PasswordInput;
import com.securevault.ui.components.PasswordStrengthBar;
import com.securevault.util.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Arrays;

/**
 * First-run screen: creates the master account.
 */
public class RegisterFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField usernameField = new JTextField(22);
    private final PasswordInput passwordInput = new PasswordInput(22);
    private final PasswordInput confirmInput = new PasswordInput(22);
    private final PasswordStrengthBar strengthBar = new PasswordStrengthBar();
    private final JLabel matchLabel = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final JLabel messageLabel = Theme.label(" ", Theme.SMALL, Theme.DANGER);
    private final JButton createButton = Theme.primaryButton("Create Vault");

    public RegisterFrame(AppController controller) {
        super(controller, "Create Vault");
        buildUI();
        finishSetup(new Dimension(460, 600));
        setResizable(false);
        SwingUtilities.invokeLater(usernameField::requestFocusInWindow);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(new HeaderPanel(AppConfig.APP_NAME, "Set up your password vault"), BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), Theme.padding(22, 28, 22, 28)));

        JPanel intro = new JPanel(new BorderLayout(0, 4));
        intro.setOpaque(false);
        intro.add(Theme.label("Create your master account", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        intro.add(Theme.label("<html>The master password protects everything. Choose one you will "
                + "remember - it cannot be recovered.</html>", Theme.SMALL, Theme.MUTED), BorderLayout.CENTER);
        card.add(intro, BorderLayout.NORTH);

        FormPanel form = new FormPanel();
        form.addField("Username", usernameField);
        form.addField("Master password", passwordInput);
        form.addRow(strengthBar, 6);
        form.addField("Confirm master password", confirmInput);
        form.addRow(matchLabel, 4);
        form.addRow(Theme.label("<html>At least " + AppConfig.MIN_MASTER_PASSWORD_LENGTH
                + " characters, mixing upper/lower case, digits and symbols.</html>",
                Theme.SMALL, Theme.MUTED), 10);
        form.addRow(messageLabel, 8);
        JPanel formHolder = new JPanel(new BorderLayout());   // keeps the form at the top
        formHolder.setOpaque(false);
        formHolder.add(form, BorderLayout.NORTH);
        card.add(formHolder, BorderLayout.CENTER);

        createButton.setPreferredSize(new Dimension(10, 38));
        createButton.addActionListener(e -> attemptRegister());
        card.add(createButton, BorderLayout.SOUTH);

        passwordInput.onChange(this::refreshIndicators);
        confirmInput.onChange(this::refreshIndicators);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(Theme.padding(18));
        wrapper.add(card, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        getRootPane().setDefaultButton(createButton);
    }

    /** Live feedback while typing: strength bar and "passwords match" label. */
    private void refreshIndicators() {
        strengthBar.update(passwordInput.getText());
        if (confirmInput.isEmpty()) {
            matchLabel.setText(" ");
        } else if (passwordInput.getText().equals(confirmInput.getText())) {
            setMatch("Passwords match", Theme.SUCCESS);
        } else {
            setMatch("Passwords do not match", Theme.DANGER);
        }
    }

    private void setMatch(String text, Color color) {
        matchLabel.setText(text);
        matchLabel.setForeground(color);
    }

    private void attemptRegister() {
        String username = usernameField.getText();
        char[] password = passwordInput.getPassword();
        char[] confirm = confirmInput.getPassword();
        setFormEnabled(false);
        setBusy(true);
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setText("Creating your vault...");

        BackgroundTask.run(() -> controller.getAuthService().register(username, password, confirm),
                session -> {
                    wipe(password, confirm);
                    setBusy(false);
                    openVault(session);
                },
                error -> {
                    wipe(password, confirm);
                    setBusy(false);
                    setFormEnabled(true);
                    if (!ErrorHandler.isExpected(error)) {
                        AppLogger.error("Registration failed", error);
                    }
                    messageLabel.setForeground(Theme.DANGER);
                    messageLabel.setText("<html>" + ErrorHandler.friendlyMessage(error) + "</html>");
                });
    }

    private void openVault(Session session) {
        VaultService vault = new VaultService(controller.getStorage(), session.getEncryptionService());
        showInfo("Your vault is ready, " + session.getUser().getUsername() + "!\n"
                + "Data is stored encrypted in:\n" + AppConfig.DATA_FOLDER);
        controller.showDashboard(session, vault);
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        confirmInput.setEnabled(enabled);
        createButton.setEnabled(enabled);
    }

    private static void wipe(char[]... arrays) {
        for (char[] array : arrays) {
            Arrays.fill(array, '\0');
        }
    }
}
