package com.securevault.ui.dialogs;

import com.securevault.service.AuthService;
import com.securevault.service.Session;
import com.securevault.service.VaultService;
import com.securevault.ui.BackgroundTask;
import com.securevault.ui.Theme;
import com.securevault.ui.components.FormPanel;
import com.securevault.ui.components.PasswordInput;
import com.securevault.ui.components.PasswordStrengthBar;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Window;
import java.util.Arrays;

/**
 * Changes the master password. The whole vault is re-encrypted with the new key.
 */
public class ChangePasswordDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    private final transient AuthService auth;
    private final transient VaultService vault;
    private final transient Session session;

    private final PasswordInput currentInput = new PasswordInput(20);
    private final PasswordInput newInput = new PasswordInput(20);
    private final PasswordInput confirmInput = new PasswordInput(20);
    private final PasswordStrengthBar strengthBar = new PasswordStrengthBar();
    private final JButton changeButton = Theme.primaryButton("Change Password");
    private final JButton cancelButton = Theme.secondaryButton("Cancel");

    private transient Session newSession;

    public ChangePasswordDialog(Window owner, AuthService auth, VaultService vault, Session session) {
        super(owner, "Change Master Password");
        this.auth = auth;
        this.vault = vault;
        this.session = session;
        buildUI();
        finishSetup();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.SURFACE);
        content.setBorder(Theme.padding(20));

        FormPanel form = new FormPanel();
        form.addField("Current master password", currentInput);
        form.addField("New master password", newInput);
        form.addRow(strengthBar, 6);
        form.addField("Confirm new password", confirmInput);
        content.add(form, BorderLayout.CENTER);

        newInput.onChange(() -> strengthBar.update(newInput.getText()));

        cancelButton.addActionListener(e -> dispose());
        changeButton.addActionListener(e -> change());
        content.add(buttonBar(cancelButton, changeButton), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(changeButton);
        setContentPane(content);
    }

    private void change() {
        char[] current = currentInput.getPassword();
        char[] next = newInput.getPassword();
        char[] confirm = confirmInput.getPassword();
        setWorking(true);
        showNotice("Re-encrypting your vault...");

        BackgroundTask.run(() -> auth.changeMasterPassword(session, vault, current, next, confirm),
                result -> {
                    wipe(current, next, confirm);
                    setWorking(false);
                    newSession = result;
                    dispose();
                },
                error -> {
                    wipe(current, next, confirm);
                    setWorking(false);
                    showMessage(error.getMessage());
                });
    }

    private void setWorking(boolean working) {
        changeButton.setEnabled(!working);
        cancelButton.setEnabled(!working);
        setCursor(working ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private static void wipe(char[]... arrays) {
        for (char[] a : arrays) {
            Arrays.fill(a, '\0');
        }
    }

    /** The new session after a successful change, or null. */
    public Session getNewSession() {
        return newSession;
    }
}
