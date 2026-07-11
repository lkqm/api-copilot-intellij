package io.apix.window.debug;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import io.apix.debug.DebugHttpRequest;
import io.apix.util.OpenApiUtils;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for editing query parameters as an editable table, with bulk-edit support.
 * Columns: [☑] Name | Value | Type | Description | ×
 *
 * Interaction (Postman-style):
 * - No +/- toolbar buttons.
 * - A ghost row always sits at the bottom with placeholder text.
 * - Typing in the ghost row's Key cell promotes it to a real row and appends a new ghost row.
 * - Each real row has an inline × delete button in the last column.
 */
public class QueryParamPanel extends BulkEditablePanel {

    static final String[] PARAM_TYPES = {"string", "integer", "number", "boolean", "array", "object"};

    private static final int COL_ENABLED = 0;
    private static final int COL_NAME    = 1;
    private static final int COL_VALUE   = 2;
    private static final int COL_TYPE    = 3;
    private static final int COL_DESC    = 4;
    private static final int COL_DELETE  = 5;

    private final DefaultTableModel tableModel;
    private final JBTable table;
    private boolean suppressGhostAdd = false;

    public QueryParamPanel() {
        tableModel = new DefaultTableModel(new Object[]{"", "Name", "Value", "Type", "Description", ""}, 0) {
            @Override public Class<?> getColumnClass(int col) {
                return col == COL_ENABLED ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int col) {
                if (col == COL_DELETE) return false;
                if (col == COL_DESC) return false;
                if (col == COL_ENABLED && isGhostRow(row)) return false;
                return true;
            }
        };

        // When the last ghost row receives key or value input, promote it and append a new ghost row.
        tableModel.addTableModelListener(e -> {
            if (suppressGhostAdd || (e.getColumn() != COL_NAME && e.getColumn() != COL_VALUE)) return;
            int lastRow = tableModel.getRowCount() - 1;
            if (lastRow >= 0) {
                String name = (String) tableModel.getValueAt(lastRow, COL_NAME);
                String value = (String) tableModel.getValueAt(lastRow, COL_VALUE);
                if ((name != null && !name.isEmpty()) || (value != null && !value.isEmpty())) {
                    // Ensure promoted row is enabled
                    if (!Boolean.TRUE.equals(tableModel.getValueAt(lastRow, COL_ENABLED))) {
                        tableModel.setValueAt(Boolean.TRUE, lastRow, COL_ENABLED);
                    }
                    suppressGhostAdd = true;
                    addRow(false, "", "", "string", "");
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
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(JBUI.scale(110));
        table.getColumnModel().getColumn(COL_VALUE).setPreferredWidth(JBUI.scale(200));
        table.getColumnModel().getColumn(COL_DESC).setPreferredWidth(JBUI.scale(160));
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

        // No selection / focus highlighting; show type as placeholder when value is empty
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

        // Click on × column to delete the row
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

    public void setParams(List<Parameter> specParams) {
        suppressGhostAdd = true;
        tableModel.setRowCount(0);
        if (specParams != null) {
            for (Parameter p : specParams) {
                String type = OpenApiUtils.schemaTypeDisplay(p.getSchema());
                String desc = p.getDescription() != null ? p.getDescription() : "";
                addRow(true, p.getName(), "", type, desc);
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

    public List<DebugHttpRequest.QueryParam> getQueryParams() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        List<DebugHttpRequest.QueryParam> result = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (isGhostRow(i)) continue;
            boolean enabled = Boolean.TRUE.equals(tableModel.getValueAt(i, COL_ENABLED));
            String  name    = (String) tableModel.getValueAt(i, COL_NAME);
            String  value   = (String) tableModel.getValueAt(i, COL_VALUE);
            if (name != null && !name.isEmpty()) {
                result.add(new DebugHttpRequest.QueryParam(enabled, name, value != null ? value : ""));
            }
        }
        return result;
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
            addRow(Boolean.parseBoolean(parsed[2]), parsed[0], parsed[1], "string", "");
        }
        suppressGhostAdd = false;
        ensureGhostRow();
    }

    // ── Private ───────────────────────────────────────────────────────────

    private boolean isGhostRow(int row) {
        return row == tableModel.getRowCount() - 1
                && isBlank((String) tableModel.getValueAt(row, COL_NAME))
                && isBlank((String) tableModel.getValueAt(row, COL_VALUE));
    }

    private void ensureGhostRow() {
        if (tableModel.getRowCount() == 0 || !isGhostRow(tableModel.getRowCount() - 1)) {
            suppressGhostAdd = true;
            addRow(false, "", "", "string", "");
            suppressGhostAdd = false;
        }
    }

    private void addRow(boolean enabled, String name, String value, String type, String desc) {
        tableModel.addRow(new Object[]{enabled, name, value, type, desc, null});
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    /** Renders the delete column: shows × for real rows, empty for ghost row. */
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
