package com.securevault.ui;

import com.securevault.model.Credential;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects the list of credentials to the JTable.
 * The table asks this model what to show in each cell.
 */
public class CredentialTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_WEBSITE = 0;
    public static final int COL_USERNAME = 1;
    public static final int COL_PASSWORD = 2;
    public static final int COL_CATEGORY = 3;
    public static final int COL_UPDATED = 4;

    private static final String[] COLUMNS = {"Website", "Username", "Password", "Category", "Last updated"};
    private static final String MASK = "••••••••";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final List<Credential> rows = new ArrayList<>();
    private boolean showPasswords = false;

    public void setCredentials(List<Credential> credentials) {
        rows.clear();
        rows.addAll(credentials);
        fireTableDataChanged();
    }

    public Credential getCredentialAt(int modelRow) {
        return rows.get(modelRow);
    }

    /** Finds the row of a credential, or -1. */
    public int indexOf(String id) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isShowPasswords() {
        return showPasswords;
    }

    public void setShowPasswords(boolean showPasswords) {
        this.showPasswords = showPasswords;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == COL_UPDATED ? LocalDateTime.class : String.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;   // editing happens in the Edit dialog
    }

    @Override
    public Object getValueAt(int row, int column) {
        Credential c = rows.get(row);
        switch (column) {
            case COL_WEBSITE:
                return c.getWebsite();
            case COL_USERNAME:
                return c.getUsername();
            case COL_PASSWORD:
                return showPasswords ? c.getPassword() : MASK;
            case COL_CATEGORY:
                return c.getCategory();
            case COL_UPDATED:
                return c.getUpdatedAt();
            default:
                return "";
        }
    }

    public static String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_FORMAT.format(dateTime);
    }
}
