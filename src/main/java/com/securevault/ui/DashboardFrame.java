package com.securevault.ui;

import com.securevault.config.AppConfig;
import com.securevault.exception.VaultException;
import com.securevault.model.Credential;
import com.securevault.service.SecurityAnalyzer;
import com.securevault.service.SecurityReport;
import com.securevault.service.Session;
import com.securevault.service.VaultService;
import com.securevault.ui.components.CredentialCard;
import com.securevault.ui.components.FilterChip;
import com.securevault.ui.components.HeaderPanel;
import com.securevault.ui.components.SearchField;
import com.securevault.ui.components.SecurityBanner;
import com.securevault.ui.components.VerticalListPanel;
import com.securevault.ui.dialogs.ChangePasswordDialog;
import com.securevault.ui.dialogs.CredentialDialog;
import com.securevault.ui.dialogs.PasswordGeneratorDialog;
import com.securevault.util.ClipboardManager;
import com.securevault.util.InputValidator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main window shown after unlocking.
 *
 * Layout (top to bottom):
 *   header with the user name and a Lock button
 *   search box + "Add Password" button
 *   category chips (All, Email, Social, ...)
 *   security banner (weak / reused passwords)
 *   list of password cards
 *   status bar
 */
public class DashboardFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;
    private static final String ALL = "All";
    private static final String VIEW_LIST = "list";
    private static final String VIEW_EMPTY = "empty";
    private static final String VIEW_NO_RESULTS = "noResults";

    protected transient Session session;
    protected final transient VaultService vault;
    private final transient SecurityAnalyzer analyzer = new SecurityAnalyzer();
    private transient SecurityReport report;

    private final HeaderPanel header;
    protected final SearchField searchField = new SearchField("Search passwords...", 24);
    private final JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final SecurityBanner banner = new SecurityBanner(this::toggleReview);
    private final VerticalListPanel listPanel = new VerticalListPanel();
    private final JPanel views = new JPanel(new CardLayout());
    private final JLabel countLabel = Theme.label("", Theme.SMALL, Theme.MUTED);
    private final JLabel statusLabel = Theme.label(" ", Theme.SMALL, Theme.SUCCESS);
    private final Timer statusResetTimer = new Timer(4000, e -> setStatus(" ", Theme.SUCCESS));
    private final transient ClipboardManager clipboard = new ClipboardManager();
    private final transient AutoLockManager autoLock;

    private final List<CredentialCard> cards = new ArrayList<>();
    private String selectedId;
    private String categoryFilter = ALL;
    private boolean reviewing = false;

    // ---------- Actions (shared by buttons, menus and shortcuts) ----------
    protected final Action addAction = action("Add Password", e -> addEntry());
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
        super(controller, "My Passwords");
        this.session = session;
        this.vault = vault;
        this.header = new HeaderPanel(AppConfig.APP_NAME, "Signed in as " + session.getUser().getUsername());
        this.autoLock = new AutoLockManager(AppConfig.AUTO_LOCK_MINUTES, this::autoLockNow);
        statusResetTimer.setRepeats(false);

        buildUI();
        setJMenuBar(buildMenuBar());
        installEventHandlers();
        refresh(null);
        finishSetup(new Dimension(900, 640));
        autoLock.start();

        if (vault.wasRecoveredFromBackup()) {
            SwingUtilities.invokeLater(() -> showInfo(
                    "Your vault file was damaged, so it was restored from the automatic backup.\n"
                    + "Changes made after the last successful save may be missing."));
        }
    }

    private static Action action(String name, Consumer<ActionEvent> handler) {
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

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(Theme.padding(16, 20, 12, 20));
        body.add(buildTopSection(), BorderLayout.NORTH);

        listPanel.setBackground(Theme.SURFACE);
        listPanel.setFocusable(true);
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        views.setOpaque(false);
        views.add(scroll, VIEW_LIST);
        views.add(buildEmptyState(), VIEW_EMPTY);
        views.add(buildNoResultsState(), VIEW_NO_RESULTS);
        body.add(views, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Theme.SURFACE);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.padding(6, 20, 6, 20)));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(countLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JComponent buildTopSection() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Row 1: search + add button
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        searchField.setFont(Theme.BODY.deriveFont(14f));
        searchField.setPreferredSize(new Dimension(200, 38));
        searchField.setToolTipText("Search by website, username, category or notes (Ctrl+F)");
        searchField.onChange(this::renderList);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(addButton(), BorderLayout.EAST);
        searchRow.setAlignmentX(LEFT_ALIGNMENT);
        top.add(searchRow);
        top.add(Box.createVerticalStrut(12));

        // Row 2: category chips
        chipsRow.setOpaque(false);
        chipsRow.setBorder(Theme.padding(0, -8, 0, 0));
        chipsRow.setAlignmentX(LEFT_ALIGNMENT);
        top.add(chipsRow);
        top.add(Box.createVerticalStrut(12));

        // Row 3: security banner
        banner.setAlignmentX(LEFT_ALIGNMENT);
        top.add(banner);
        return top;
    }

    private JButton addButton() {
        JButton button = Theme.primaryButton("+ Add Password");
        button.addActionListener(addAction);
        button.setFont(Theme.BODY.deriveFont(Font.BOLD, 14f));
        button.setPreferredSize(new Dimension(170, 38));
        button.setFocusable(false);
        return button;
    }

    private JComponent buildEmptyState() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel(new ImageIcon(Theme.lockIcon(72)));
        JLabel title = Theme.label("No passwords saved yet", Theme.TITLE.deriveFont(20f), Theme.TEXT);
        JLabel hint = Theme.label("<html><center>Add your first password. It is encrypted and "
                + "stored only on this computer.</center></html>", Theme.BODY, Theme.MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        JButton add = addButton();

        for (JComponent c : new JComponent[]{icon, title, hint, add}) {
            c.setAlignmentX(CENTER_ALIGNMENT);
        }
        content.add(icon);
        content.add(Box.createVerticalStrut(14));
        content.add(title);
        content.add(Box.createVerticalStrut(6));
        content.add(hint);
        content.add(Box.createVerticalStrut(18));
        content.add(add);
        panel.add(content);
        return panel;
    }

    private JComponent buildNoResultsState() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = Theme.label("No passwords match", Theme.HEADING, Theme.TEXT);
        JLabel hint = Theme.label("Try a different search or category.", Theme.BODY, Theme.MUTED);
        JButton clear = Theme.secondaryButton("Clear filters");
        clear.addActionListener(e -> clearFilters());
        for (JComponent c : new JComponent[]{title, hint, clear}) {
            c.setAlignmentX(CENTER_ALIGNMENT);
        }
        content.add(title);
        content.add(Box.createVerticalStrut(6));
        content.add(hint);
        content.add(Box.createVerticalStrut(14));
        content.add(clear);
        panel.add(content);
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

    // =====================================================================
    // Event handling: shortcuts, keyboard navigation, auto-lock
    // =====================================================================

    private void installEventHandlers() {
        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        setShortcut(addAction, KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl));
        setShortcut(editAction, KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrl));
        setShortcut(deleteAction, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        setShortcut(copyUserAction, KeyStroke.getKeyStroke(KeyEvent.VK_B, ctrl));
        setShortcut(copyPasswordAction, KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl | InputEvent.SHIFT_DOWN_MASK));
        setShortcut(focusSearchAction, KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl));
        setShortcut(generatorAction, KeyStroke.getKeyStroke(KeyEvent.VK_G, ctrl));
        setShortcut(lockAction, KeyStroke.getKeyStroke(KeyEvent.VK_L, ctrl));
        setShortcut(exitAction, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ctrl));
        setShortcut(aboutAction, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

        // Keyboard navigation inside the list
        bindList(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "next", e -> moveSelection(1));
        bindList(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "previous", e -> moveSelection(-1));
        bindList(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open", e -> editSelected());
        bindList(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl), "copyPassword", e -> copySelected(true));

        // Search box: Escape clears it, Down arrow jumps into the list
        bindSearch(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch", e -> searchField.setText(""));
        bindSearch(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "toList", e -> {
            if (!cards.isEmpty()) {
                listPanel.requestFocusInWindow();
                if (selectedId == null) {
                    select(cards.get(0).getCredential());
                }
            }
        });

        // Clear the clipboard when the window is minimised
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                clipboard.clearIfUnchanged();
            }
        });
    }

    private void bindList(KeyStroke key, String name, Consumer<ActionEvent> handler) {
        listPanel.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
        listPanel.getActionMap().put(name, action(name, handler));
    }

    private void bindSearch(KeyStroke key, String name, Consumer<ActionEvent> handler) {
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
        searchField.getActionMap().put(name, action(name, handler));
    }

    private void setShortcut(Action action, KeyStroke key) {
        action.putValue(Action.ACCELERATOR_KEY, key);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, action.getValue(Action.NAME));
        getRootPane().getActionMap().put(action.getValue(Action.NAME), action);
    }

    /** Called by the AutoLockManager after the idle time. */
    private void autoLockNow() {
        for (java.awt.Window w : getOwnedWindows()) {
            w.dispose();   // close any open dialog first so nothing stays on screen
        }
        controller.lock();
    }

    /** What happens when the user uses a card. */
    private final transient CredentialCard.Listener cardListener = new CredentialCard.Listener() {
        @Override
        public void onSelect(Credential credential) {
            select(credential);
            listPanel.requestFocusInWindow();
        }

        @Override
        public void onOpen(Credential credential) {
            editSelected();
        }

        @Override
        public void onCopyPassword(Credential credential) {
            copySelected(true);
        }

        @Override
        public void onShowMenu(CredentialCard card, Component invoker, int x, int y) {
            buildCardMenu(card).show(invoker, x, y);
        }
    };

    private JPopupMenu buildCardMenu(CredentialCard card) {
        Credential c = card.getCredential();
        JPopupMenu menu = new JPopupMenu();

        menu.add(menuItem("Copy username", e -> copySelected(false)));
        menu.add(menuItem("Copy password", e -> copySelected(true)));
        menu.add(menuItem(card.isRevealed() ? "Hide password" : "Show password",
                e -> card.setRevealed(!card.isRevealed())));

        JMenuItem open = menuItem("Open website", e -> openWebsite(c));
        open.setEnabled(InputValidator.toWebAddress(c.getWebsite()) != null
                && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
        menu.add(open);

        menu.addSeparator();
        menu.add(menuItem("Edit", e -> editSelected()));
        JMenuItem delete = menuItem("Delete", e -> deleteSelected());
        delete.setForeground(Theme.DANGER);
        menu.add(delete);
        return menu;
    }

    private static JMenuItem menuItem(String text, Consumer<ActionEvent> handler) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(handler::accept);
        return item;
    }

    // =====================================================================
    // Showing the data
    // =====================================================================

    /** Re-reads the vault, recomputes the security report and redraws everything. */
    protected void refresh(String selectId) {
        if (selectId != null) {
            selectedId = selectId;
        }
        report = analyzer.analyze(vault.getAll());
        if (!report.hasIssues()) {
            reviewing = false;
        }
        rebuildChips();
        renderList();
        scrollToSelected();
    }

    private void rebuildChips() {
        List<String> used = new ArrayList<>();
        for (Credential c : vault.getAll()) {
            if (used.stream().noneMatch(u -> u.equalsIgnoreCase(c.getCategory()))) {
                used.add(c.getCategory());
            }
        }
        used.sort(String.CASE_INSENSITIVE_ORDER);
        if (!ALL.equals(categoryFilter) && used.stream().noneMatch(u -> u.equalsIgnoreCase(categoryFilter))) {
            categoryFilter = ALL;   // that category no longer exists
        }

        chipsRow.removeAll();
        ButtonGroup group = new ButtonGroup();
        List<String> names = new ArrayList<>();
        names.add(ALL);
        names.addAll(used);
        for (String name : names) {
            FilterChip chip = new FilterChip(name);
            chip.setSelected(name.equalsIgnoreCase(categoryFilter));
            chip.addActionListener(e -> {
                categoryFilter = name;
                renderList();
            });
            group.add(chip);
            chipsRow.add(chip);
        }
        chipsRow.setVisible(!used.isEmpty());
        chipsRow.revalidate();
        chipsRow.repaint();
    }

    /** Draws the cards that match the search, category and review filters. */
    protected void renderList() {
        String search = searchField.getText();
        List<Credential> shown = new ArrayList<>();
        for (Credential c : vault.getAll()) {
            boolean categoryOk = ALL.equals(categoryFilter) || c.getCategory().equalsIgnoreCase(categoryFilter);
            boolean reviewOk = !reviewing || report.needsAttention(c.getId());
            if (categoryOk && reviewOk && c.matches(search)) {
                shown.add(c);
            }
        }

        if (selectedId != null && shown.stream().noneMatch(c -> c.getId().equals(selectedId))) {
            selectedId = null;
        }

        listPanel.removeAll();
        cards.clear();
        for (Credential c : shown) {
            CredentialCard card = new CredentialCard(c, report.isWeak(c.getId()), report.isReused(c.getId()),
                    cardListener);
            card.setSelected(c.getId().equals(selectedId));
            cards.add(card);
            listPanel.add(card);
        }
        listPanel.revalidate();
        listPanel.repaint();

        String view = vault.count() == 0 ? VIEW_EMPTY : (shown.isEmpty() ? VIEW_NO_RESULTS : VIEW_LIST);
        ((CardLayout) views.getLayout()).show(views, view);

        banner.update(report, reviewing);
        updateCount(shown.size());
        updateActionStates();
    }

    private void updateCount(int shown) {
        int total = vault.count();
        String noun = total == 1 ? " password" : " passwords";
        countLabel.setText(shown == total ? total + noun : shown + " of " + total + noun);
    }

    protected void updateActionStates() {
        boolean selected = getSelectedCredential() != null;
        editAction.setEnabled(selected);
        deleteAction.setEnabled(selected);
        copyUserAction.setEnabled(selected);
        copyPasswordAction.setEnabled(selected);
    }

    private void select(Credential credential) {
        selectedId = credential == null ? null : credential.getId();
        for (CredentialCard card : cards) {
            card.setSelected(card.getCredential().getId().equals(selectedId));
        }
        updateActionStates();
    }

    private void moveSelection(int step) {
        if (cards.isEmpty()) {
            return;
        }
        int index = -1;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getCredential().getId().equals(selectedId)) {
                index = i;
            }
        }
        int next = Math.max(0, Math.min(cards.size() - 1, index + step));
        select(cards.get(next).getCredential());
        scrollToSelected();
    }

    private void scrollToSelected() {
        SwingUtilities.invokeLater(() -> {
            for (CredentialCard card : cards) {
                if (card.getCredential().getId().equals(selectedId)) {
                    listPanel.scrollRectToVisible(card.getBounds());
                }
            }
        });
    }

    protected Credential getSelectedCredential() {
        if (selectedId == null) {
            return null;
        }
        return vault.findById(selectedId).orElse(null);
    }

    private void toggleReview() {
        reviewing = !reviewing;
        if (reviewing) {
            searchField.setText("");
            categoryFilter = ALL;
            rebuildChips();
        }
        renderList();
    }

    private void clearFilters() {
        reviewing = false;
        categoryFilter = ALL;
        searchField.setText("");
        rebuildChips();
        renderList();
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
            clearFilters();
            refresh(dialog.getSaved().getId());
            setStatus("Saved " + dialog.getSaved().getWebsite(), Theme.SUCCESS);
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
            refresh(dialog.getSaved().getId());
            setStatus("Saved changes to " + dialog.getSaved().getWebsite(), Theme.SUCCESS);
        }
    }

    protected void deleteSelected() {
        Credential selected = getSelectedCredential();
        if (selected == null) {
            return;
        }
        if (!confirm("Delete the password for " + selected.getDisplayName() + "?\nThis cannot be undone.",
                "Delete Password")) {
            return;
        }
        try {
            vault.delete(selected.getId());
            selectedId = null;
            refresh(null);
            setStatus("Deleted " + selected.getWebsite(), Theme.SUCCESS);
        } catch (VaultException e) {
            ErrorHandler.show(this, "Delete", e);
        }
    }

    protected void copySelected(boolean password) {
        Credential selected = getSelectedCredential();
        if (selected == null) {
            return;
        }
        String text = password ? selected.getPassword() : selected.getUsername();
        copyToClipboard(text, (password ? "Password" : "Username") + " for " + selected.getWebsite() + " copied");
    }

    protected void copyToClipboard(String text, String message) {
        clipboard.copy(text);
        setStatus(message + " - clears in " + AppConfig.CLIPBOARD_CLEAR_SECONDS + " seconds", Theme.SUCCESS);
    }

    private void openWebsite(Credential credential) {
        String address = InputValidator.toWebAddress(credential.getWebsite());
        if (address == null) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(address));
            setStatus("Opened " + credential.getWebsite() + " in your browser", Theme.SUCCESS);
        } catch (Exception e) {
            ErrorHandler.show(this, "Open website", e);
        }
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
        autoLock.stop();
        clipboard.clearIfUnchanged();
        super.dispose();
    }
}
