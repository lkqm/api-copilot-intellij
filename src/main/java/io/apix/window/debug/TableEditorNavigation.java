package io.apix.window.debug;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

final class TableEditorNavigation {

    private static final String ACTION_TAB_FORWARD = "apix.table.tab.forward";
    private static final String ACTION_TAB_BACKWARD = "apix.table.tab.backward";

    private TableEditorNavigation() {}

    static void install(JTable table) {
        installAction(table, ACTION_TAB_FORWARD, false, 0);
        installAction(table, ACTION_TAB_BACKWARD, true, 0);
        installAction(table, ACTION_TAB_FORWARD, false, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        installAction(table, ACTION_TAB_BACKWARD, true, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private static void installAction(JTable table, String actionKey, boolean backward, int condition) {
        InputMap inputMap = table.getInputMap(condition);
        if (inputMap == null) return;
        inputMap.put(KeyStroke.getKeyStroke("TAB"), ACTION_TAB_FORWARD);
        inputMap.put(KeyStroke.getKeyStroke("shift TAB"), ACTION_TAB_BACKWARD);
        table.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToAdjacentEditableCell(table, backward);
            }
        });
    }

    private static void moveToAdjacentEditableCell(JTable table, boolean backward) {
        int row = table.isEditing() ? table.getEditingRow() : table.getSelectedRow();
        int col = table.isEditing() ? table.getEditingColumn() : table.getSelectedColumn();
        if (row < 0 || col < 0) return;

        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null && !editor.stopCellEditing()) return;
            row = table.getSelectedRow() >= 0 ? table.getSelectedRow() : row;
            col = table.getSelectedColumn() >= 0 ? table.getSelectedColumn() : col;
        }

        Point nextCell = findAdjacentEditableCell(table, row, col, backward);
        if (nextCell == null) return;

        table.changeSelection(nextCell.x, nextCell.y, false, false);
        if (!table.editCellAt(nextCell.x, nextCell.y)) return;

        Component editorComponent = table.getEditorComponent();
        if (editorComponent != null) {
            editorComponent.requestFocusInWindow();
            if (editorComponent instanceof JTextField) {
                ((JTextField) editorComponent).selectAll();
            }
        }
    }

    private static Point findAdjacentEditableCell(JTable table, int row, int col, boolean backward) {
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        int index = row * columnCount + col;
        int step = backward ? -1 : 1;

        for (int i = index + step; i >= 0 && i < rowCount * columnCount; i += step) {
            int candidateRow = i / columnCount;
            int candidateCol = i % columnCount;
            if (table.isCellEditable(candidateRow, candidateCol)) {
                return new Point(candidateRow, candidateCol);
            }
        }
        return null;
    }
}
