package com.securevault.ui.components;

import com.securevault.model.Credential;
import com.securevault.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One saved password shown as a row:
 *   [G]  gmail.com  (Email) (Weak)                  [Copy password] [⋮]
 *        student.demo@gmail.com
 */
public class CredentialCard extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String MASK = "••••••••••";

    /** What the card asks the dashboard to do when the user interacts with it. */
    public interface Listener {
        void onSelect(Credential credential);

        void onOpen(Credential credential);

        void onCopyPassword(Credential credential);

        void onShowMenu(CredentialCard card, Component invoker, int x, int y);
    }

    private final transient Credential credential;
    private final transient Listener listener;
    private final JLabel passwordLabel = new JLabel();
    private final JButton menuButton = new JButton("⋮");
    private boolean selected;
    private boolean hovered;
    private boolean revealed;

    public CredentialCard(Credential credential, boolean weak, boolean reused, Listener listener) {
        super(new BorderLayout(14, 0));
        this.credential = credential;
        this.listener = listener;
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), Theme.padding(12, 18, 12, 14)));

        add(new SiteAvatar(credential.getWebsite(), 42), BorderLayout.WEST);
        add(buildText(weak, reused), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.EAST);

        installMouseHandling(this);
        setToolTipText("Double-click to edit");
    }

    private JComponent buildText(boolean weak, boolean reused) {
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        JLabel website = Theme.label(credential.getWebsite(), Theme.CARD_TITLE, Theme.TEXT);
        website.setBorder(Theme.padding(0, 0, 0, 2));
        titleRow.add(website);
        titleRow.add(Tag.category(credential.getCategory()));
        if (weak) {
            titleRow.add(Tag.warning("Weak"));
        }
        if (reused) {
            titleRow.add(Tag.warning("Reused"));
        }
        titleRow.setAlignmentX(LEFT_ALIGNMENT);
        // FlowLayout adds a gap on the left; pull the row back so text lines up
        titleRow.setBorder(Theme.padding(0, -6, 0, 0));
        text.add(titleRow);

        JLabel username = Theme.label(credential.getUsername(), Theme.BODY, Theme.MUTED);
        username.setBorder(Theme.padding(4, 0, 0, 0));
        username.setAlignmentX(LEFT_ALIGNMENT);
        text.add(username);

        passwordLabel.setFont(Theme.MONO);
        passwordLabel.setForeground(Theme.TEXT);
        passwordLabel.setBorder(Theme.padding(4, 0, 0, 0));
        passwordLabel.setAlignmentX(LEFT_ALIGNMENT);
        passwordLabel.setVisible(false);
        text.add(passwordLabel);

        // keep the text block vertically centred next to the avatar
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(text, new java.awt.GridBagConstraints(0, 0, 1, 1, 1, 1,
                java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.HORIZONTAL,
                new java.awt.Insets(0, 0, 0, 0), 0, 0));
        return holder;
    }

    private JComponent buildButtons() {
        JButton copy = Theme.secondaryButton("Copy password");
        copy.setFocusable(false);
        copy.setToolTipText("Copy the password (clears automatically)");
        copy.addActionListener(e -> {
            listener.onSelect(credential);
            listener.onCopyPassword(credential);
        });

        menuButton.setFocusable(false);
        menuButton.setToolTipText("More options");
        menuButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuButton.setPreferredSize(new Dimension(40, copy.getPreferredSize().height));
        menuButton.addActionListener(e -> {
            listener.onSelect(credential);
            listener.onShowMenu(this, menuButton, 0, menuButton.getHeight());
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(copy);
        buttons.add(menuButton);

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(buttons);
        return holder;
    }

    /** Click selects, double-click opens, right-click shows the menu, hover highlights. */
    private void installMouseHandling(Component component) {
        if (component instanceof JButton) {
            return;   // buttons handle their own clicks
        }
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                listener.onSelect(credential);
                maybeShowMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowMenu(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    listener.onOpen(credential);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), CredentialCard.this);
                setHovered(contains(p));
            }

            private void maybeShowMenu(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    listener.onShowMenu(CredentialCard.this, e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                installMouseHandling(child);
            }
        }
    }

    // ---------------- State ----------------

    public Credential getCredential() {
        return credential;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateBackground();
    }

    public boolean isRevealed() {
        return revealed;
    }

    /** Shows or hides the password under the username. */
    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
        passwordLabel.setText(revealed ? credential.getPassword() : MASK);
        passwordLabel.setVisible(revealed);
        revalidate();
        repaint();
    }

    private void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            updateBackground();
        }
    }

    private void updateBackground() {
        Color bg = selected ? Theme.PRIMARY_LIGHT : (hovered ? Theme.HOVER : Theme.SURFACE);
        setBackground(bg);
        repaint();
    }

    /** Three vertical dots, drawn in code so they look the same with every font. */
    private static final class DotsIcon implements javax.swing.Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? Theme.TEXT : Theme.MUTED);
            for (int i = 0; i < 3; i++) {
                g2.fillOval(x, y + i * 6, 4, 4);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 4;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (selected) {
            g.setColor(Theme.PRIMARY);
            g.fillRect(0, 0, 4, getHeight());   // accent bar on the left
        }
    }
}
