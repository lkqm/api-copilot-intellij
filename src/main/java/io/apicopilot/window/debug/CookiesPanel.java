package io.apicopilot.window.debug;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import io.apicopilot.util.OpenApiUtils;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.swing.*;
import java.util.List;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel for editing request cookies as an editable table, with bulk-edit support.
 * Columns: [☑] Name | Value | Description | ×
 *
 * <p>Postman-style: ghost row at bottom, inline × delete, no +/- toolbar.
 * Bulk Edit button lives in the column-header row (right side).</p>
 *
 * <p>Cookies are serialized into a single {@code Cookie: name1=value1; name2=value2} header
 * via {@link #getCookieHeader()}.</p>
 */
public class CookiesPanel extends BulkEditablePanel {

    private static final int COL_ENABLED = 0;
    private static final int COL_NAME    = 1;
    private static final int COL_VALUE   = 2;
    private static final int COL_TYPE    = 3;
    private static final int COL_DESC    = 4;
    private static final int COL_DELETE  = 5;

    private final DefaultTableModel tableModel;
    private final JBTable table;
    private boolean suppressGhostAdd = false;

    public CookiesPanel() {
        tableModel = new DefaultTableModel(new Object[]{"", "Name", "Value", "Type", "Description", ""}, 0) {
            @Override public Class<?> getColumnClass(int col) {
                return col == COL_ENABLED ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int col) {
                if (col == COL_DELETE) return false;
                if (col == COL_ENABLED && isGhostRow(row)) return false;
                return true;
            }
        };

        tableModel.addTableModelListener(e -> {
            if (suppressGhostAdd || e.getColumn() != COL_NAME) return;
            int lastRow = tableModel.getRowCount() - 1;
            if (lastRow >= 0) {
                String name = (String) tableModel.getValueAt(lastRow, COL_NAME);
                if (name != null && !name.isEmpty()) {
                    if (!Boolean.TRUE.equals(tableModel.getValueAt(lastRow, COL_ENABLED))) {
                        tableModel.setValueAt(Boolean.TRUE, lastRow, COL_ENABLED);
                    }
                    suppressGhostAdd = true;
                    tableModel.addRow(new Object[]{false, "", "", "string", "", null});
                    suppressGhostAdd = false;
                }
            }
        });

        table = new JBTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) c.setBackground(getBackground());
                return c;
            }
        };
        table.setRowHeight(JBUI.scale(24));
        table.getColumnModel().getColumn(COL_ENABLED).setMaxWidth(JBUI.scale(30));
        table.getColumnModel().getColumn(COL_ENABLED).setMinWidth(JBUI.scale(30));
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(JBUI.scale(120));
        table.getColumnModel().getColumn(COL_VALUE).setPreferredWidth(JBUI.scale(200));
        table.getColumnModel().getColumn(COL_DESC).setPreferredWidth(JBUI.scale(180));
        table.getColumnModel().getColumn(COL_DELETE).setMaxWidth(JBUI.scale(28));
        table.getColumnModel().getColumn(COL_DELETE).setMinWidth(JBUI.scale(28));
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(COL_DELETE).setCellRenderer(new DeleteButtonRenderer());
        table.removeColumn(table.getColumnModel().getColumn(COL_TYPE));

        // Single-click to edit; no border highlight on active cell
        JTextField editorField = new JTextField();
        editorField.setBorder(JBUI.Borders.empty(0, 4));
        DefaultCellEditor singleClickEditor = new DefaultCellEditor(editorField);
        singleClickEditor.setClickCountToStart(1);
        table.setDefaultEditor(String.class, singleClickEditor);
        TableEditorNavigation.install(table);

        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                if (t.convertColumnIndexToModel(col) == COL_VALUE
                        && !isGhostRow(row)
                        && (value == null || ((String) value).isEmpty())) {
                    String type = (String) tableModel.getValueAt(row, COL_TYPE);
                    if (type != null && !type.isEmpty()) {
                        super.getTableCellRendererComponent(t, type, false, false, row, col);
                        setForeground(UIManager.getColor("Label.disabledForeground"));
                        return this;
                    }
                }
                super.getTableCellRendererComponent(t, value, false, false, row, col);
                setForeground(t.getForeground());
                return this;
            }
        });
        table.setDefaultRenderer(Boolean.class, new TableCellRenderer() {
            private final JCheckBox cb = new JCheckBox();
            { cb.setHorizontalAlignment(SwingConstants.CENTER); cb.setOpaque(true); cb.setFocusable(false); }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                cb.setSelected(Boolean.TRUE.equals(value));
                cb.setBackground(t.getBackground());
                cb.setEnabled(!isGhostRow(row));
                return cb;
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (table.convertColumnIndexToModel(col) == COL_DELETE && row >= 0 && !isGhostRow(row)) {
                    if (table.isEditing()) table.getCellEditor().stopCellEditing();
                    suppressGhostAdd = true;
                    tableModel.removeRow(row);
                    suppressGhostAdd = false;
                    ensureGhostRow();
                }
            }
        });

        ensureGhostRow();
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setFocusable(false);
        initTableCard(scrollPane);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setSpecCookies(List<Parameter> specParams) {
        suppressGhostAdd = true;
        tableModel.setRowCount(0);
        if (specParams != null) {
            for (Parameter p : specParams) {
                String type = OpenApiUtils.schemaTypeDisplay(p.getSchema());
                String desc = p.getDescription() != null ? p.getDescription() : "";
                tableModel.addRow(new Object[]{true, p.getName(), "", type, desc, null});
            }
        }
        suppressGhostAdd = false;
        ensureGhostRow();
    }

    public boolean hasValues() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (isGhostRow(i)) continue;
            String name = (String) tableModel.getValueAt(i, COL_NAME);
            if (name != null && !name.isEmpty()) return true;
        }
        return false;
    }

    public void addValueChangeListener(Runnable listener) {
        tableModel.addTableModelListener(e -> listener.run());
    }

    /**
     * Returns the formatted {@code Cookie} header value, e.g. {@code session=abc; token=xyz},
     * or {@code null} if no enabled cookies with non-empty names exist.
     */
    public String getCookieHeader() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (isGhostRow(i)) continue;
            boolean enabled = Boolean.TRUE.equals(tableModel.getValueAt(i, COL_ENABLED));
            String  name    = (String) tableModel.getValueAt(i, COL_NAME);
            String  value   = (String) tableModel.getValueAt(i, COL_VALUE);
            if (!enabled || name == null || name.isEmpty()) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(name).append('=').append(value != null ? value : "");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ── BulkEditablePanel ─────────────────────────────────────────────────

    @Override
    protected String toBulkText() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (isGhostRow(i)) continue;
            boolean enabled = Boolean.TRUE.equals(tableModel.getValueAt(i, COL_ENABLED));
            String  name    = (String) tableModel.getValueAt(i, COL_NAME);
            String  value   = (String) tableModel.getValueAt(i, COL_VALUE);
            sb.append(toBulkLine(enabled, name, value)).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    @Override
    protected void fromBulkText(String text) {
        suppressGhostAdd = true;
        tableModel.setRowCount(0);
        for (String line : text.split("\n", -1)) {
            String[] parsed = parseBulkLine(line);
            if (parsed == null) continue;
            tableModel.addRow(new Object[]{Boolean.parseBoolean(parsed[2]), parsed[0], parsed[1], "string", "", null});
        }
        suppressGhostAdd = false;
        ensureGhostRow();
    }

    // ── Private ───────────────────────────────────────────────────────────

    private boolean isGhostRow(int row) {
        return row == tableModel.getRowCount() - 1
                && "".equals(tableModel.getValueAt(row, COL_NAME));
    }

    private void ensureGhostRow() {
        if (tableModel.getRowCount() == 0 || !isGhostRow(tableModel.getRowCount() - 1)) {
            suppressGhostAdd = true;
            tableModel.addRow(new Object[]{false, "", "", "", null});
            suppressGhostAdd = false;
        }
    }

    private class DeleteButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    t, value, false, false, row, col);
            label.setHorizontalAlignment(JLabel.CENTER);
            if (isGhostRow(row)) {
                label.setText("");
            } else {
                label.setText("×");
                Color fg = UIManager.getColor("Label.foreground");
                label.setForeground(fg != null ? fg : Color.DARK_GRAY);
            }
            return label;
        }
    }
}
