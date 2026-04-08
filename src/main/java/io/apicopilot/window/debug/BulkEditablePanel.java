package io.apicopilot.window.debug;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Base panel providing a toggle between table view and bulk-edit text view.
 *
 * <pre>
 * ┌──── KEY ─────── VALUE ─────── DESC ──── [Bulk Edit] ─┐
 * │  ← table rows                                         │
 * │    or                                                  │
 * │  ← bulk card  (plain-text area)                        │
 * └────────────────────────────────────────────────────────┘
 * </pre>
 *
 * The "Bulk Edit" toggle sits at the right end of the table column-header row
 * (Postman-style). When switched to bulk mode, the button moves to a thin strip
 * above the text area so it remains reachable.
 *
 * Subclasses must:
 * <ol>
 *   <li>Call {@link #initTableCard(JScrollPane)} at the end of their constructor.</li>
 *   <li>Implement {@link #toBulkText()} to serialize table rows → text.</li>
 *   <li>Implement {@link #fromBulkText(String)} to parse text → table rows.</li>
 * </ol>
 *
 * <h3>Bulk text format</h3>
 * <pre>
 * # comment line (skipped)
 * Key: Value          ← enabled row
 * // Key: Value       ← disabled row
 * </pre>
 */
public abstract class BulkEditablePanel extends JPanel {

    private static final String CARD_TABLE = "table";
    private static final String CARD_BULK  = "bulk";

    private static final String PLACEHOLDER =
            "# One entry per line:  Key: Value\n# Prefix with // to disable:  // Key: Value";

    private final CardLayout    cardLayout     = new CardLayout();
    private final JPanel        cardPanel      = new JPanel(cardLayout);
    private final JBTextArea     bulkArea;
    private final JToggleButton bulkBtn        = new JToggleButton("Bulk Edit");

    /** Composite panel wrapping the JTableHeader + bulkBtn (table mode). */
    private JPanel tableHeaderWrapper;
    /** Thin strip above the bulk text area that hosts bulkBtn in bulk mode. */
    private final JPanel bulkHeaderStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));

    protected BulkEditablePanel() {
        super(new BorderLayout());

        // ── Bulk text area with placeholder hint ──────────────────────────
        bulkArea = new JBTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    Color ph = UIManager.getColor("TextField.placeholderForeground");
                    g2.setColor(ph != null ? ph : UIManager.getColor("Label.disabledForeground"));
                    Insets ins = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    int x = ins.left + 4;
                    int y = ins.top + fm.getAscent() + 4;
                    for (String line : PLACEHOLDER.split("\n")) {
                        g2.drawString(line, x, y);
                        y += fm.getHeight();
                    }
                    g2.dispose();
                }
            }
        };
        bulkArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(12f)));
        bulkArea.setLineWrap(false);
        bulkArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { bulkArea.repaint(); }
            public void removeUpdate(DocumentEvent e) { bulkArea.repaint(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        // ── Bulk card: thin strip (button in bulk mode) + text area ───────
        bulkHeaderStrip.setBorder(JBUI.Borders.customLine(
                UIManager.getColor("Separator.separatorColor"), 0, 0, 1, 0));

        JBScrollPane bulkScroll = new JBScrollPane(bulkArea);
        bulkScroll.setBorder(JBUI.Borders.empty());

        JPanel bulkCard = new JPanel(new BorderLayout());
        bulkCard.add(bulkHeaderStrip, BorderLayout.NORTH);
        bulkCard.add(bulkScroll,      BorderLayout.CENTER);
        cardPanel.add(bulkCard, CARD_BULK);

        // ── Toggle button: flat / text-link style ─────────────────────────
        bulkBtn.setFont(bulkBtn.getFont().deriveFont(11f));
        bulkBtn.setFocusable(false);
        bulkBtn.setBorderPainted(false);
        bulkBtn.setContentAreaFilled(false);
        bulkBtn.setOpaque(false);

        bulkBtn.addActionListener(e -> {
            if (bulkBtn.isSelected()) {
                // Table → Bulk: move button from table header to bulk strip
                bulkBtn.setText("Key-Value Edit");
                bulkArea.setText(toBulkText());
                bulkArea.setCaretPosition(0);
                if (tableHeaderWrapper != null) {
                    tableHeaderWrapper.remove(bulkBtn);
                    tableHeaderWrapper.revalidate();
                    tableHeaderWrapper.repaint();
                }
                bulkHeaderStrip.add(bulkBtn);
                bulkHeaderStrip.revalidate();
                cardLayout.show(cardPanel, CARD_BULK);
            } else {
                // Bulk → Table: move button back into table header
                bulkBtn.setText("Bulk Edit");
                fromBulkText(bulkArea.getText());
                bulkHeaderStrip.remove(bulkBtn);
                bulkHeaderStrip.revalidate();
                if (tableHeaderWrapper != null) {
                    tableHeaderWrapper.add(bulkBtn, BorderLayout.EAST);
                    tableHeaderWrapper.revalidate();
                    tableHeaderWrapper.repaint();
                }
                cardLayout.show(cardPanel, CARD_TABLE);
            }
        });

        add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Fallback for subclasses that pass a plain JPanel (e.g. ToolbarDecorator panels).
     * The Bulk Edit button is placed in a top bar above the component.
     */
    protected final void initTableCard(JComponent tableComponent) {
        if (tableComponent instanceof JScrollPane) {
            initTableCard((JScrollPane) tableComponent);
            return;
        }
        // Legacy path: button in a top bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        topBar.setBorder(JBUI.Borders.customLine(UIManager.getColor("Separator.separatorColor"), 0, 0, 1, 0));
        topBar.add(bulkBtn);
        // Wrap in a panel so the top bar is always visible
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(topBar,          BorderLayout.NORTH);
        wrapper.add(tableComponent,  BorderLayout.CENTER);
        cardPanel.add(wrapper, CARD_TABLE);
        cardLayout.show(cardPanel, CARD_TABLE);
    }

    /**
     * Register the table-view scroll pane as the default card and embed the
     * Bulk Edit button into the scroll pane's column-header row (right side).
     * Must be called once at the end of the subclass constructor.
     *
     * The header wrapping is deferred via AncestorListener because
     * JTable.configureEnclosingScrollPane() runs on addNotify (after construction)
     * and would otherwise overwrite any column header we set here.
     */
    protected final void initTableCard(JScrollPane scrollPane) {
        // Defer header wrapping until the table is actually added to the UI hierarchy
        Component view = scrollPane.getViewport().getView();
        if (view instanceof JTable) {
            JTable table = (JTable) view;
            table.addAncestorListener(new AncestorListener() {
                @Override
                public void ancestorAdded(AncestorEvent event) {
                    wrapColumnHeader(scrollPane);
                    table.removeAncestorListener(this);
                }
                @Override public void ancestorRemoved(AncestorEvent event) {}
                @Override public void ancestorMoved(AncestorEvent event) {}
            });
        }

        cardPanel.add(scrollPane, CARD_TABLE);
        cardLayout.show(cardPanel, CARD_TABLE);
    }

    /** Wraps the scroll pane's current column header with a composite panel that includes bulkBtn. */
    private void wrapColumnHeader(JScrollPane scrollPane) {
        JViewport columnHeaderViewport = scrollPane.getColumnHeader();
        if (columnHeaderViewport == null) return;
        Component originalHeader = columnHeaderViewport.getView();
        // Guard: already wrapped
        if (originalHeader == tableHeaderWrapper) return;

        tableHeaderWrapper = new JPanel(new BorderLayout()) {
            @Override
            public Color getBackground() {
                Color c = UIManager.getColor("TableHeader.background");
                return c != null ? c : super.getBackground();
            }
        };
        tableHeaderWrapper.setOpaque(true);
        tableHeaderWrapper.add(originalHeader, BorderLayout.CENTER);
        tableHeaderWrapper.add(bulkBtn,        BorderLayout.EAST);
        scrollPane.setColumnHeaderView(tableHeaderWrapper);
    }

    /** Serialize current table rows into bulk-edit text. */
    protected abstract String toBulkText();

    /** Parse bulk-edit text and repopulate the table rows. */
    protected abstract void fromBulkText(String text);

    // ── Shared parsing helpers ────────────────────────────────────────────

    /**
     * Parse one line of bulk text.
     *
     * @return {@code [name, value, enabledStr]} or {@code null} to skip this line.
     */
    protected static String[] parseBulkLine(String raw) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) return null;

        boolean enabled = true;
        if (line.startsWith("//")) {
            enabled = false;
            line = line.substring(2).trim();
        }

        int colon = line.indexOf(':');
        String name  = (colon >= 0 ? line.substring(0, colon) : line).trim();
        String value = (colon >= 0 ? line.substring(colon + 1) : "").trim();

        if (name.isEmpty()) return null;
        return new String[]{name, value, enabled ? "true" : "false"};
    }

    /** Serialize one row (with enable flag) to a bulk-text line. */
    protected static String toBulkLine(boolean enabled, String name, String value) {
        String line = (name != null ? name : "") + ": " + (value != null ? value : "");
        return enabled ? line : "// " + line;
    }
}
