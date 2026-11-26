package com.securevault.ui;

import com.securevault.config.AppConfig;
import com.securevault.exception.VaultException;
import com.securevault.model.Credential;
import com.securevault.service.Session;
import com.securevault.service.VaultService;
import com.securevault.ui.components.HeaderPanel;
import com.securevault.ui.components.SearchField;
import com.securevault.ui.dialogs.ChangePasswordDialog;
import com.securevault.ui.dialogs.CredentialDialog;
import com.securevault.ui.dialogs.PasswordGeneratorDialog;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main window shown after unlocking: a searchable table of all saved
 * credentials with buttons and menus to add, edit, delete and copy them.
 */
public class DashboardFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;
    private static final String ALL_CATEGORIES = "All categories";
    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    protected transient Session session;
    protected final transient VaultService vault;

    protected final CredentialTableModel tableModel = new CredentialTableModel();
    protected final JTable table = new JTable(tableModel) {
        private static final long serialVersionUID = 1L;

        /** Zebra stripes for easier reading. */
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            if (!isRowSelected(row)) {
                c.setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT);
                c.setForeground(Theme.TEXT);
            }
            return c;
        }
    };
    private final TableRowSorter<CredentialTableModel> sorter = new TableRowSorter<>(tableModel);
    protected final SearchField searchField = new SearchField("Search website, username, category or notes...", 24);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JToggleButton revealToggle = new JToggleButton("Show Passwords");
    private final HeaderPanel header;
    private final JPanel centerCards = new JPanel(new CardLayout());
    private final JLabel countLabel = Theme.label("", Theme.SMALL, Theme.MUTED);
    private final JLabel statusLabel = Theme.label(" ", Theme.SMALL, Theme.SUCCESS);
    private final Timer statusResetTimer = new Timer(4000, e -> setStatus(" ", Theme.SUCCESS));

    // ---------- Actions (shared by buttons and menus) ----------
    protected final Action addAction = action("Add", e -> addEntry());
    protected final Action editAction = action("Edit", e -> editSelected());
    protected final Action deleteAction = action("Delete", e -> deleteSelected());
    protected final Action copyUserAction = action("Copy Username", e -> copySelected(false));
    protected final Action copyPasswordAction = action("Copy Password", e -> copySelected(true));
    protected final Action generatorAction = action("Password Generator", e -> openGenerator());
    protected final Action changeMasterAction = action("Change Master Password...", e -> changeMasterPassword());
    protected final Action lockAction = action("Lock", e -> controller.lock());
    protected final Action exitAction = action("Exit", e -> controller.exit());
    protected final Action aboutAction = action("About", e -> showAbout());
    protected final Action focusSearchAction = action("Find", e -> searchField.requestFocusInWindow());

    public DashboardFrame(AppController controller, Session session, VaultService vault) {
        super(controller, "Vault");
        this.session = session;
        this.vault = vault;
        this.header = new HeaderPanel(AppConfig.APP_NAME, "");
        statusResetTimer.setRepeats(false);

        buildUI();
        setJMenuBar(buildMenuBar());
        refreshTable(null);
        finishSetup(new Dimension(960, 600));
    }

    private static Action action(String name, java.util.function.Consumer<ActionEvent> handler) {
        return new AbstractAction(name) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                handler.accept(e);
            }
        };
    }

    // =====================================================================
    // Building the window
    // =====================================================================

    private void buildUI() {
        setLayout(new BorderLayout());

        JButton lockButton = new JButton(lockAction);
        lockButton.setText("Lock Vault");
        lockButton.setFocusable(false);
        header.setRightComponent(lockButton);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(Theme.padding(14, 16, 8, 16));
        body.add(buildToolbar(), BorderLayout.NORTH);

        configureTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        centerCards.add(scroll, CARD_TABLE);
        centerCards.add(buildEmptyState(), CARD_EMPTY);
        body.add(centerCards, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Theme.SURFACE);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.padding(6, 16, 6, 16)));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(countLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel filters = new JPanel(new BorderLayout(8, 0));
        filters.setOpaque(false);
        searchField.onChange(this::applyFilter);
        filters.add(searchField, BorderLayout.CENTER);
        categoryFilter.addActionListener(e -> applyFilter());
        categoryFilter.setPrototypeDisplayValue("Entertainment....");
        filters.add(categoryFilter, BorderLayout.EAST);
        toolbar.add(filters, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton addButton = Theme.primaryButton("+ Add");
        addButton.setAction(addAction);
        addButton.setText("+ Add");
        buttons.add(addButton);
        buttons.add(new JButton(editAction));
        buttons.add(new JButton(deleteAction));
        buttons.add(new JButton(copyUserAction));
        buttons.add(new JButton(copyPasswordAction));
        revealToggle.addActionListener(e -> {
            tableModel.setShowPasswords(revealToggle.isSelected());
            revealToggle.setText(revealToggle.isSelected() ? "Hide Passwords" : "Show Passwords");
        });
        buttons.add(revealToggle);
        JButton generatorButton = new JButton(generatorAction);
        generatorButton.setText("Generator");
        buttons.add(generatorButton);
        toolbar.add(buttons, BorderLayout.EAST);
        return toolbar;
    }

    private void configureTable() {
        table.setRowSorter(sorter);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.BODY.deriveFont(Font.BOLD));
        table.getSelectionModel().addListSelectionListener(e -> updateActionStates());

        table.setDefaultRenderer(String.class, new PaddedRenderer());

        DefaultTableCellRenderer dateRenderer = new PaddedRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                super.setValue(CredentialTableModel.formatDate((LocalDateTime) value));
            }
        };
        table.setDefaultRenderer(LocalDateTime.class, dateRenderer);

        PaddedRenderer passwordRenderer = new PaddedRenderer(Theme.MONO);
        table.getColumnModel().getColumn(CredentialTableModel.COL_PASSWORD).setCellRenderer(passwordRenderer);

        int[] widths = {200, 220, 170, 120, 150};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private JComponent buildEmptyState() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        JLabel title = Theme.label("Your vault is empty", Theme.HEADING, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setVerticalAlignment(SwingConstants.BOTTOM);
        JLabel hint = Theme.label("Click \"+ Add\" to save your first password.", Theme.BODY, Theme.MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setVerticalAlignment(SwingConstants.TOP);
        panel.add(title, BorderLayout.CENTER);
        panel.add(hint, BorderLayout.SOUTH);
        hint.setPreferredSize(new Dimension(10, 200));
        return panel;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(addAction);
        file.addSeparator();
        file.add(lockAction);
        file.add(exitAction);
        bar.add(file);

        JMenu edit = new JMenu("Edit");
        edit.add(editAction);
        edit.add(deleteAction);
        edit.addSeparator();
        edit.add(copyUserAction);
        edit.add(copyPasswordAction);
        edit.addSeparator();
        edit.add(focusSearchAction);
        bar.add(edit);

        JMenu tools = new JMenu("Tools");
        tools.add(generatorAction);
        tools.add(changeMasterAction);
        bar.add(tools);

        JMenu help = new JMenu("Help");
        help.add(aboutAction);
        bar.add(help);
        return bar;
    }

    /** Table cells with a little space on the left and right (and an optional fixed font). */
    private static class PaddedRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final Font fixedFont;

        PaddedRenderer() {
            this(null);
        }

        PaddedRenderer(Font fixedFont) {
            this.fixedFont = fixedFont;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (fixedFont != null) {
                setFont(fixedFont);
            }
            setBorder(Theme.padding(0, 8, 0, 8));
            return this;
        }
    }

    // =====================================================================
    // Refreshing
    // =====================================================================

    /** Reloads the table from the vault and re-selects the given entry (if any). */
    protected void refreshTable(String selectId) {
        tableModel.setCredentials(vault.getAll());
        refreshCategories();
        applyFilter();

        boolean empty = vault.count() == 0;
        ((CardLayout) centerCards.getLayout()).show(centerCards, empty ? CARD_EMPTY : CARD_TABLE);
        header.setSubtitle("Signed in as " + session.getUser().getUsername());

        if (selectId != null) {
            int modelRow = tableModel.indexOf(selectId);
            if (modelRow >= 0) {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                }
            }
        }
        updateActionStates();
    }

    private void refreshCategories() {
        Object selected = categoryFilter.getSelectedItem();
        categoryFilter.removeAllItems();
        categoryFilter.addItem(ALL_CATEGORIES);
        for (String category : vault.getCategories()) {
            categoryFilter.addItem(category);
        }
        categoryFilter.setSelectedItem(selected != null ? selected : ALL_CATEGORIES);
    }

    /** Filters the table by the search text and the selected category. */
    private void applyFilter() {
        String text = searchField.getText();
        Object category = categoryFilter.getSelectedItem();
        List<RowFilter<CredentialTableModel, Integer>> filters = new ArrayList<>();

        filters.add(new RowFilter<CredentialTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends CredentialTableModel, ? extends Integer> entry) {
                return entry.getModel().getCredentialAt(entry.getIdentifier()).matches(text);
            }
        });
        if (category != null && !ALL_CATEGORIES.equals(category)) {
            filters.add(new RowFilter<CredentialTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends CredentialTableModel, ? extends Integer> entry) {
                    return entry.getModel().getCredentialAt(entry.getIdentifier())
                            .getCategory().equalsIgnoreCase(category.toString());
                }
            });
        }
        sorter.setRowFilter(RowFilter.andFilter(filters));
        updateCount();
        updateActionStates();
    }

    private void updateCount() {
        int shown = table.getRowCount();
        int total = vault.count();
        String noun = total == 1 ? " entry" : " entries";
        countLabel.setText(shown == total ? total + noun : shown + " of " + total + noun);
    }

    protected void updateActionStates() {
        boolean selected = getSelectedCredential() != null;
        editAction.setEnabled(selected);
        deleteAction.setEnabled(selected);
        copyUserAction.setEnabled(selected);
        copyPasswordAction.setEnabled(selected);
    }

    protected Credential getSelectedCredential() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return tableModel.getCredentialAt(table.convertRowIndexToModel(viewRow));
    }

    protected void setStatus(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
        if (!" ".equals(text)) {
            statusResetTimer.restart();
        }
    }

    // =====================================================================
    // User actions
    // =====================================================================

    protected void addEntry() {
        CredentialDialog dialog = new CredentialDialog(this, vault, null,
                vault.getCategories(AppConfig.DEFAULT_CATEGORIES));
        dialog.setVisible(true);
        if (dialog.getSaved() != null) {
            refreshTable(dialog.getSaved().getId());
            setStatus("Added " + dialog.getSaved().getWebsite(), Theme.SUCCESS);
        }
    }

    protected void editSelected() {
        Credential selected = getSelectedCredential();
        if (selected == null) {
            return;
        }
        CredentialDialog dialog = new CredentialDialog(this, vault, selected,
                vault.getCategories(AppConfig.DEFAULT_CATEGORIES));
        dialog.setVisible(true);
        if (dialog.getSaved() != null) {
            refreshTable(dialog.getSaved().getId());
            setStatus("Saved changes to " + dialog.getSaved().getWebsite(), Theme.SUCCESS);
        }
    }

    protected void deleteSelected() {
        Credential selected = getSelectedCredential();
        if (selected == null) {
            return;
        }
        if (!confirm("Delete the entry for " + selected.getDisplayName() + "?\nThis cannot be undone.",
                "Delete Entry")) {
            return;
        }
        try {
            vault.delete(selected.getId());
            refreshTable(null);
            setStatus("Deleted " + selected.getWebsite(), Theme.SUCCESS);
        } catch (VaultException e) {
            showError("Could not delete the entry:\n" + e.getMessage());
        }
    }

    protected void copySelected(boolean password) {
        Credential selected = getSelectedCredential();
        if (selected == null) {
            return;
        }
        String text = password ? selected.getPassword() : selected.getUsername();
        copyToClipboard(text, (password ? "Password" : "Username") + " for " + selected.getWebsite()
                + " copied to clipboard");
    }

    protected void copyToClipboard(String text, String message) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        setStatus(message, Theme.SUCCESS);
    }

    protected void openGenerator() {
        new PasswordGeneratorDialog(this, false).setVisible(true);
    }

    protected void changeMasterPassword() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(this, controller.getAuthService(), vault, session);
        dialog.setVisible(true);
        if (dialog.getNewSession() != null) {
            session = dialog.getNewSession();
            showInfo("Master password changed. Use the new password next time you unlock.");
        }
    }

    private void showAbout() {
        showInfo(AppConfig.APP_NAME + " " + AppConfig.APP_VERSION + "\n\n"
                + "A desktop password manager built with Java Swing.\n"
                + "Vault encryption: AES-256-GCM\n"
                + "Key derivation: PBKDF2-HMAC-SHA256\n\n"
                + "Data folder:\n" + AppConfig.DATA_FOLDER);
    }

    @Override
    public void dispose() {
        statusResetTimer.stop();
        super.dispose();
    }
}
