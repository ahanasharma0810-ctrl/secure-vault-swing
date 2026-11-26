package com.securevault.ui.dialogs;

import com.securevault.exception.ValidationException;
import com.securevault.exception.VaultException;
import com.securevault.model.Credential;
import com.securevault.service.VaultService;
import com.securevault.ui.Theme;
import com.securevault.ui.components.FormPanel;
import com.securevault.ui.components.PasswordInput;
import com.securevault.ui.components.PasswordStrengthBar;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;

/**
 * Add / Edit form for one credential.
 * Pass null as 'existing' to add a new entry.
 */
public class CredentialDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    private final transient VaultService vault;
    private final transient Credential existing;

    private final JTextField websiteField = new JTextField(26);
    private final JTextField usernameField = new JTextField(26);
    private final PasswordInput passwordInput = new PasswordInput(18);
    private final PasswordStrengthBar strengthBar = new PasswordStrengthBar();
    private final JComboBox<String> categoryBox;
    private final JTextArea notesArea = new JTextArea(4, 26);

    private transient Credential saved;

    public CredentialDialog(Window owner, VaultService vault, Credential existing, List<String> categories) {
        super(owner, existing == null ? "Add Entry" : "Edit Entry");
        this.vault = vault;
        this.existing = existing;
        this.categoryBox = new JComboBox<>(categories.toArray(new String[0]));
        buildUI();
        fillFields();
        finishSetup();
        setSize(new Dimension(Math.max(getWidth(), 460), getHeight()));
        SwingUtilities.invokeLater(websiteField::requestFocusInWindow);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.SURFACE);
        content.setBorder(Theme.padding(20));

        JButton generateButton = Theme.secondaryButton("Generate");
        generateButton.addActionListener(e -> openGenerator());
        JPanel passwordRow = new JPanel(new BorderLayout(6, 0));
        passwordRow.setOpaque(false);
        passwordRow.add(passwordInput, BorderLayout.CENTER);
        passwordRow.add(generateButton, BorderLayout.EAST);

        categoryBox.setEditable(true);   // the user may type a new category
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        FormPanel form = new FormPanel();
        form.addField("Website or app *", websiteField);
        form.addField("Username or email *", usernameField);
        form.addField("Password *", passwordRow);
        form.addRow(strengthBar, 6);
        form.addField("Category", categoryBox);
        form.addGrowingField("Notes", new JScrollPane(notesArea));
        content.add(form, BorderLayout.CENTER);

        passwordInput.onChange(() -> strengthBar.update(passwordInput.getText()));

        JButton cancel = Theme.secondaryButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton save = Theme.primaryButton(existing == null ? "Add" : "Save");
        save.addActionListener(e -> save());
        content.add(buttonBar(cancel, save), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);

        setContentPane(content);
    }

    private void fillFields() {
        if (existing == null) {
            categoryBox.setSelectedItem(Credential.DEFAULT_CATEGORY);
            return;
        }
        websiteField.setText(existing.getWebsite());
        usernameField.setText(existing.getUsername());
        passwordInput.setText(existing.getPassword());
        categoryBox.setSelectedItem(existing.getCategory());
        notesArea.setText(existing.getNotes());
        strengthBar.update(existing.getPassword());
    }

    private void openGenerator() {
        PasswordGeneratorDialog generator = new PasswordGeneratorDialog(this, true);
        generator.setVisible(true);   // waits here until closed (modal)
        if (generator.getResult() != null) {
            passwordInput.setText(generator.getResult());
            passwordInput.setRevealed(true);
        }
    }

    private void save() {
        String website = websiteField.getText();
        String username = usernameField.getText();
        String password = passwordInput.getText();
        Object category = categoryBox.getEditor().getItem();
        String notes = notesArea.getText();
        try {
            if (existing == null) {
                saved = vault.add(website, username, password, String.valueOf(category), notes);
            } else {
                saved = vault.update(existing.getId(), website, username, password, String.valueOf(category), notes);
            }
            dispose();
        } catch (ValidationException e) {
            showMessage(e.getMessage());
        } catch (VaultException e) {
            showMessage("Could not save: " + e.getMessage());
        }
    }

    /** The saved credential, or null if cancelled. */
    public Credential getSaved() {
        return saved;
    }
}
