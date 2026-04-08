package io.apicopilot.window.debug;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import io.apicopilot.util.OpenApiUtils;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Panel for editing path parameters extracted from the URL template.
 * Columns: [☑] Name | Value | Type | Description
 * (no ghost row / delete button — path params are fixed by the URL template)
 */
public class PathParamPanel extends JPanel {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    private static final int COL_ENABLED = 0;
    private static final int COL_NAME    = 1;
    private static final int COL_VALUE   = 2;
    private static final int COL_TYPE    = 3;
    private static final int COL_DESC    = 4;

    private final DefaultTableModel tableModel;
    private final JBTable table;

    public PathParamPanel() {
        super(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"", "Name", "Value", "Type", "Description"}, 0) {
            @Override public Class<?> getColumnClass(int col) {
                return col == COL_ENABLED ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int col) {
                return col == COL_VALUE || col == COL_ENABLED;
            }
        };

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
        table.getTableHeader().setReorderingAllowed(false);
        table.removeColumn(table.getColumnModel().getColumn(COL_TYPE));

        // Single-click to edit; no border highlight on active cell
        JTextField editorField = new JTextField();
        editorField.setBorder(JBUI.Borders.empty(0, 4));
        DefaultCellEditor singleClickEditor = new DefaultCellEditor(editorField);
        singleClickEditor.setClickCountToStart(1);
        table.setDefaultEditor(String.class, singleClickEditor);
        TableEditorNavigation.install(table);

        // Show type as placeholder when value is empty
        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                if (t.convertColumnIndexToModel(col) == COL_VALUE
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
                return cb;
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setFocusable(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setParams(String path, List<Parameter> specParams) {
        tableModel.setRowCount(0);
        // Build a map from spec for type/desc lookup
        Map<String, Parameter> specMap = new LinkedHashMap<>();
        if (specParams != null) {
            for (Parameter p : specParams) specMap.put(p.getName(), p);
        }
        for (String name : extractParamNames(path)) {
            Parameter p = specMap.get(name);
            String type = p != null ? OpenApiUtils.schemaTypeDisplay(p.getSchema()) : "string";
            String desc = p != null && p.getDescription() != null ? p.getDescription() : "";
            tableModel.addRow(new Object[]{Boolean.TRUE, name, "", type, desc});
        }
    }

    public boolean hasValues() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String value = (String) tableModel.getValueAt(i, COL_VALUE);
            if (value != null && !value.isEmpty()) return true;
        }
        return false;
    }

    public void addValueChangeListener(Runnable listener) {
        tableModel.addTableModelListener(e -> listener.run());
    }

    public Map<String, String> getValues() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = Boolean.TRUE.equals(tableModel.getValueAt(i, COL_ENABLED));
            String  name    = (String) tableModel.getValueAt(i, COL_NAME);
            String  value   = (String) tableModel.getValueAt(i, COL_VALUE);
            if (enabled && name != null && !name.isEmpty()) {
                result.put(name, value != null ? value : "");
            }
        }
        return result;
    }

    // ── Private ───────────────────────────────────────────────────────────

    private static List<String> extractParamNames(String path) {
        List<String> names = new ArrayList<>();
        if (path == null) return names;
        Matcher m = PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) names.add(m.group(1));
        return names;
    }
}
