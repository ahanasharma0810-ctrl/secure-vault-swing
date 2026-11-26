package com.securevault.ui.components;

import com.securevault.ui.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Builds simple forms: each row is a label above a field.
 * Hides the GridBagLayout details from the windows that use it.
 */
public class FormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private int row = 0;

    public FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    /** Adds a label and, below it, the field. */
    public FormPanel addField(String labelText, JComponent field) {
        JLabel label = Theme.label(labelText, Theme.BODY.deriveFont(java.awt.Font.BOLD), Theme.TEXT);
        label.setLabelFor(field);
        add(label, constraints(new Insets(row == 0 ? 0 : 10, 0, 4, 0), 0));
        row++;
        add(field, constraints(new Insets(0, 0, 0, 0), 0));
        row++;
        return this;
    }

    /** Adds a component on its own row (hint text, strength bar, ...). */
    public FormPanel addRow(JComponent component, int topGap) {
        add(component, constraints(new Insets(topGap, 0, 0, 0), 0));
        row++;
        return this;
    }

    /** Adds a row that grows to fill the leftover height (e.g. a notes area). */
    public FormPanel addGrowingField(String labelText, JComponent field) {
        JLabel label = Theme.label(labelText, Theme.BODY.deriveFont(java.awt.Font.BOLD), Theme.TEXT);
        add(label, constraints(new Insets(10, 0, 4, 0), 0));
        row++;
        add(field, constraints(new Insets(0, 0, 0, 0), 1));
        row++;
        return this;
    }

    private GridBagConstraints constraints(Insets insets, double weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.weighty = weightY;
        c.fill = weightY > 0 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = insets;
        return c;
    }
}
