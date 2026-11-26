package com.securevault.ui.dialogs;

import com.securevault.config.AppConfig;
import com.securevault.security.PasswordGenerator;
import com.securevault.ui.Theme;
import com.securevault.ui.components.PasswordStrengthBar;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;

/**
 * Lets the user create a random password with chosen length and character types.
 * When opened from the credential dialog, "Use Password" sends the result back.
 */
public class PasswordGeneratorDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    private final transient PasswordGenerator generator = new PasswordGenerator();
    private final JTextField output = new JTextField(28);
    private final JSlider lengthSlider = new JSlider(AppConfig.MIN_GENERATED_LENGTH,
            AppConfig.MAX_GENERATED_LENGTH, AppConfig.DEFAULT_GENERATED_LENGTH);
    private final JLabel lengthLabel = Theme.label("", Theme.BODY, Theme.TEXT);
    private final JCheckBox upper = new JCheckBox("Uppercase (A-Z)", true);
    private final JCheckBox lower = new JCheckBox("Lowercase (a-z)", true);
    private final JCheckBox digits = new JCheckBox("Digits (0-9)", true);
    private final JCheckBox symbols = new JCheckBox("Symbols (!@#$...)", true);
    private final JCheckBox noAmbiguous = new JCheckBox("Avoid look-alikes (O, 0, l, 1, I)", false);
    private final PasswordStrengthBar strengthBar = new PasswordStrengthBar();

    private String result;

    /**
     * @param selectable true when the caller wants the password back ("Use Password" button)
     */
    public PasswordGeneratorDialog(Window owner, boolean selectable) {
        super(owner, "Password Generator");
        buildUI(selectable);
        regenerate();
        finishSetup();
    }

    private void buildUI(boolean selectable) {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(Theme.SURFACE);
        content.setBorder(Theme.padding(20));

        output.setFont(Theme.MONO.deriveFont(16f));
        output.setEditable(false);
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(Theme.label("Generated password", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        top.add(output, BorderLayout.CENTER);
        top.add(strengthBar, BorderLayout.SOUTH);
        content.add(top, BorderLayout.NORTH);

        JPanel options = new JPanel(new GridLayout(0, 1, 0, 4));
        options.setOpaque(false);
        options.add(lengthLabel);
        lengthSlider.setOpaque(false);
        lengthSlider.setMajorTickSpacing(10);
        lengthSlider.setPaintTicks(true);
        options.add(lengthSlider);
        for (JCheckBox box : new JCheckBox[]{upper, lower, digits, symbols, noAmbiguous}) {
            box.setOpaque(false);
            box.addActionListener(e -> regenerate());
            options.add(box);
        }
        lengthSlider.addChangeListener(e -> regenerate());
        content.add(options, BorderLayout.CENTER);

        JButton regenerateButton = Theme.secondaryButton("Regenerate");
        regenerateButton.addActionListener(e -> regenerate());
        JButton copyButton = Theme.secondaryButton("Copy");
        copyButton.addActionListener(e -> copy());

        JPanel buttons;
        if (selectable) {
            JButton useButton = Theme.primaryButton("Use Password");
            useButton.addActionListener(e -> {
                result = output.getText();
                dispose();
            });
            JButton cancel = Theme.secondaryButton("Cancel");
            cancel.addActionListener(e -> dispose());
            buttons = buttonBar(regenerateButton, cancel, useButton);
            getRootPane().setDefaultButton(useButton);
        } else {
            JButton close = Theme.secondaryButton("Close");
            close.addActionListener(e -> dispose());
            buttons = buttonBar(regenerateButton, copyButton, close);
        }
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void regenerate() {
        lengthLabel.setText("Length: " + lengthSlider.getValue() + " characters");
        try {
            String password = generator.generate(new PasswordGenerator.Options()
                    .length(lengthSlider.getValue())
                    .uppercase(upper.isSelected())
                    .lowercase(lower.isSelected())
                    .digits(digits.isSelected())
                    .symbols(symbols.isSelected())
                    .excludeAmbiguous(noAmbiguous.isSelected()));
            output.setText(password);
            strengthBar.update(password);
            showMessage(" ");
        } catch (IllegalArgumentException e) {
            output.setText("");
            strengthBar.update("");
            showMessage(e.getMessage());
        }
    }

    private void copy() {
        if (!output.getText().isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(output.getText()), null);
            showNotice("Copied to clipboard");
        }
    }

    /** The chosen password, or null if the dialog was cancelled. */
    public String getResult() {
        return result;
    }
}
