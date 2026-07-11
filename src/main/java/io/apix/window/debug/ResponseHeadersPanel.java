package io.apix.window.debug;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Map;

/**
 * Read-only panel displaying response headers as a Name / Value table.
 */
public class ResponseHeadersPanel extends JPanel {

    private static final int COL_NAME  = 0;
    private static final int COL_VALUE = 1;

    private final DefaultTableModel tableModel;

    public ResponseHeadersPanel() {
        super(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JBTable table = new JBTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) c.setBackground(getBackground());
                return c;
            }
        };
        table.setRowHeight(JBUI.scale(24));
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(JBUI.scale(180));
        table.getColumnModel().getColumn(COL_VALUE).setPreferredWidth(JBUI.scale(320));
        table.getTableHeader().setReorderingAllowed(false);

        JBScrollPane scroll = new JBScrollPane(table);
        scroll.setBorder(JBUI.Borders.empty());
        add(scroll, BorderLayout.CENTER);
    }

    public void setHeaders(Map<String, String> headers) {
        tableModel.setRowCount(0);
        if (headers != null) {
            headers.forEach((k, v) -> tableModel.addRow(new Object[]{k, v}));
        }
    }

    public void clear() {
        tableModel.setRowCount(0);
    }
}
