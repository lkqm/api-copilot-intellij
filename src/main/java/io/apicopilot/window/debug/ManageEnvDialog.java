package io.apicopilot.window.debug;

import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import io.apicopilot.debug.DebugSettings;
import io.apicopilot.debug.EnvVariable;
import io.apicopilot.debug.Environment;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Dialog for managing environments and their variables.
 *
 * <pre>
 * ┌──────────────────┬───────────────────────────────────────────┐
 * │  + New  [─]      │  dev                                      │
 * │  ─────────────   │  ──────────────────────────────────────── │
 * │  ● dev           │  [☑]  Name          Value                 │
 * │    staging       │  [☑]  baseUrl       http://localhost:8080  │
 * │    prod          │  [☑]  token         eyJhbGci...            │
 * └──────────────────┴───────────────────────────────────────────┘
 * </pre>
 */
public class ManageEnvDialog extends DialogWrapper {

    private static final int COL_ENABLED = 0;
    private static final int COL_NAME    = 1;
    private static final int COL_VALUE   = 2;

    private final Project project;
    private final String  documentId;

    // Working copy of environments (independent of persisted state until OK)
    private List<Environment> environments;

    // Left — environment list
    private final DefaultListModel<String> envListModel = new DefaultListModel<>();
    private final JBList<String>           envList      = new JBList<>(envListModel);

    // Right — variable table for the selected environment
    private final DefaultTableModel varTableModel;
    private final JBTable           varTable;
    private final JBLabel           envTitleLabel  = new JBLabel();
    private final JBTextField       baseUrlField   = new JBTextField();
    private JPanel                  rightPanel;

    private int selectedEnvIndex = -1;

    public ManageEnvDialog(Project project, String documentId) {
        super(project, true);
        this.project    = project;
        this.documentId = documentId;

        // Deep-clone environments so edits are discardable via Cancel
        DebugSettings settings = DebugSettings.getInstance(project);
        this.environments = deepClone(settings.getEnvironments(documentId));

        varTableModel = new DefaultTableModel(new Object[]{"", "Name", "Value"}, 0) {
            @Override public Class<?> getColumnClass(int col) {
                return col == COL_ENABLED ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        varTable = new JBTable(varTableModel);

        setTitle("Manage Environments");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // ── Left: environment list ─────────────────────────────────────────
        for (Environment env : environments) {
            envListModel.addElement(env.getName());
        }

        envList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        envList.addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int idx = envList.getSelectedIndex();
                if (idx >= 0 && idx != selectedEnvIndex) {
                    saveCurrentEnvVariables();
                    selectedEnvIndex = idx;
                    loadEnvVariables(idx);
                }
            }
        });
        envList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) renameSelectedEnv();
            }
        });

        envList.setDragEnabled(true);
        envList.setDropMode(DropMode.INSERT);
        envList.setTransferHandler(new EnvListTransferHandler());

        JPanel leftPanel = ToolbarDecorator.createDecorator(envList)
                .setAddAction(btn -> addEnvironment())
                .setRemoveAction(btn -> removeSelectedEnvironment())
                .disableUpDownActions()
                .createPanel();
        leftPanel.setPreferredSize(new Dimension(JBUI.scale(170), 0));

        // Select first environment
        if (!environments.isEmpty()) {
            envList.setSelectedIndex(0);
        }

        // ── Right: variable table ──────────────────────────────────────────
        varTable.setRowHeight(JBUI.scale(24));
        varTable.getColumnModel().getColumn(COL_ENABLED).setMaxWidth(JBUI.scale(30));
        varTable.getColumnModel().getColumn(COL_ENABLED).setMinWidth(JBUI.scale(30));
        varTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(JBUI.scale(150));
        varTable.getColumnModel().getColumn(COL_VALUE).setPreferredWidth(JBUI.scale(220));
        varTable.getTableHeader().setReorderingAllowed(false);

        JPanel varPanel = ToolbarDecorator.createDecorator(varTable)
                .setAddAction(btn -> addVariable())
                .setRemoveAction(btn -> removeSelectedVariable())
                .disableUpDownActions()
                .createPanel();

        envTitleLabel.setBorder(JBUI.Borders.empty(6, 8, 4, 8));
        envTitleLabel.setFont(envTitleLabel.getFont().deriveFont(Font.BOLD));
        updateEnvTitle();

        // Base URL row
        JLabel baseUrlLabel = new JLabel("Base URL:");
        baseUrlLabel.setBorder(JBUI.Borders.empty(0, 0, 0, 8));
        baseUrlField.getEmptyText().setText("e.g. https://api.example.com");

        JPanel baseUrlRow = new JPanel(new BorderLayout());
        baseUrlRow.setBorder(JBUI.Borders.empty(2, 8, 6, 8));
        baseUrlRow.add(baseUrlLabel, BorderLayout.WEST);
        baseUrlRow.add(baseUrlField, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(envTitleLabel, BorderLayout.NORTH);
        topPanel.add(baseUrlRow, BorderLayout.CENTER);

        rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(varPanel, BorderLayout.CENTER);
        rightPanel.setVisible(selectedEnvIndex >= 0);

        // ── Main panel ─────────────────────────────────────────────────────
        rightPanel.setBorder(JBUI.Borders.emptyLeft(8));
        JPanel main = new JPanel(new BorderLayout());
        main.setPreferredSize(new Dimension(JBUI.scale(660), JBUI.scale(420)));
        main.add(leftPanel, BorderLayout.WEST);
        main.add(rightPanel, BorderLayout.CENTER);
        return main;
    }

    @Override
    protected void doOKAction() {
        saveCurrentEnvVariables();
        DebugSettings settings = DebugSettings.getInstance(project);
        settings.setEnvironments(documentId, environments);
        // If the active env was deleted, clear the active env id
        if (settings.findEnvById(documentId, settings.getActiveEnvId(documentId)) == null) {
            settings.setActiveEnvId(documentId, "");
        }
        super.doOKAction();
    }

    // ── Environment list operations ────────────────────────────────────────

    private void addEnvironment() {
        String name = Messages.showInputDialog(
                project, "Environment name:", "New Environment", null);
        if (name == null || name.trim().isEmpty()) return;
        Environment env = new Environment();
        env.setId(UUID.randomUUID().toString());
        env.setName(name.trim());
        environments.add(env);
        envListModel.addElement(env.getName());
        envList.setSelectedIndex(environments.size() - 1);
    }

    private void removeSelectedEnvironment() {
        int idx = envList.getSelectedIndex();
        if (idx < 0) return;
        environments.remove(idx);
        envListModel.remove(idx);
        selectedEnvIndex = -1;
        varTableModel.setRowCount(0);
        baseUrlField.setText("");
        updateEnvTitle();
        if (!environments.isEmpty()) {
            int next = Math.min(idx, environments.size() - 1);
            envList.setSelectedIndex(next);
        }
    }

    private void renameSelectedEnv() {
        int idx = envList.getSelectedIndex();
        if (idx < 0) return;
        Environment env = environments.get(idx);
        String name = Messages.showInputDialog(
                project, "Environment name:", "Rename", null, env.getName(), null);
        if (name == null || name.trim().isEmpty()) return;
        env.setName(name.trim());
        envListModel.set(idx, env.getName());
        updateEnvTitle();
    }

    // ── Variable table operations ──────────────────────────────────────────

    private void addVariable() {
        if (selectedEnvIndex < 0) return;
        varTableModel.addRow(new Object[]{Boolean.TRUE, "", ""});
        int lastRow = varTableModel.getRowCount() - 1;
        varTable.setRowSelectionInterval(lastRow, lastRow);
        varTable.editCellAt(lastRow, COL_NAME);
    }

    private void removeSelectedVariable() {
        int row = varTable.getSelectedRow();
        if (row >= 0) {
            if (varTable.isEditing()) varTable.getCellEditor().stopCellEditing();
            varTableModel.removeRow(row);
        }
    }

    private void loadEnvVariables(int idx) {
        varTableModel.setRowCount(0);
        if (idx < 0 || idx >= environments.size()) {
            baseUrlField.setText("");
            if (rightPanel != null) rightPanel.setVisible(false);
            return;
        }
        Environment env = environments.get(idx);
        baseUrlField.setText(env.getBaseUrl() != null ? env.getBaseUrl() : "");
        for (EnvVariable v : env.getVariables()) {
            varTableModel.addRow(new Object[]{v.isEnabled(), v.getName(), v.getValue()});
        }
        updateEnvTitle();
        if (rightPanel != null) rightPanel.setVisible(true);
    }

    private void saveCurrentEnvVariables() {
        if (selectedEnvIndex < 0 || selectedEnvIndex >= environments.size()) return;
        if (varTable.isEditing()) varTable.getCellEditor().stopCellEditing();
        Environment env = environments.get(selectedEnvIndex);
        env.setBaseUrl(baseUrlField.getText().trim());
        List<EnvVariable> vars = new ArrayList<>();
        for (int i = 0; i < varTableModel.getRowCount(); i++) {
            EnvVariable v = new EnvVariable();
            v.setEnabled(Boolean.TRUE.equals(varTableModel.getValueAt(i, COL_ENABLED)));
            Object name  = varTableModel.getValueAt(i, COL_NAME);
            Object value = varTableModel.getValueAt(i, COL_VALUE);
            v.setName(name  != null ? (String) name  : "");
            v.setValue(value != null ? (String) value : "");
            vars.add(v);
        }
        env.setVariables(vars);
    }

    private void updateEnvTitle() {
        if (selectedEnvIndex >= 0 && selectedEnvIndex < environments.size()) {
            envTitleLabel.setText(environments.get(selectedEnvIndex).getName());
        } else {
            envTitleLabel.setText("Select an environment");
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private static List<Environment> deepClone(List<Environment> source) {
        Gson gson = new Gson();
        Environment[] arr = gson.fromJson(gson.toJson(source), Environment[].class);
        return arr != null ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
    }

    // ── Drag-and-drop reorder for the environment list ─────────────────────

    private class EnvListTransferHandler extends TransferHandler {
        private final DataFlavor FLAVOR = new DataFlavor(Integer.class, "Env index");
        private int dragSourceIndex = -1;

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            dragSourceIndex = envList.getSelectedIndex();
            if (dragSourceIndex < 0) return null;
            saveCurrentEnvVariables();
            return new Transferable() {
                @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{FLAVOR}; }
                @Override public boolean isDataFlavorSupported(DataFlavor f) { return FLAVOR.equals(f); }
                @Override public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
                    if (!FLAVOR.equals(f)) throw new UnsupportedFlavorException(f);
                    return dragSourceIndex;
                }
            };
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(FLAVOR);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                int from = (Integer) support.getTransferable().getTransferData(FLAVOR);
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int to = dl.getIndex();
                // Adjust target index after removal of source
                if (to > from) to--;
                if (from == to) return false;

                Environment env = environments.remove(from);
                environments.add(to, env);

                String name = envListModel.remove(from);
                envListModel.add(to, name);

                selectedEnvIndex = to;
                envList.setSelectedIndex(to);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            dragSourceIndex = -1;
        }
    }
}
