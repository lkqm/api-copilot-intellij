package io.apix.window.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import io.apix.document.DocumentSourceType;
import io.apix.document.ConnectionAuthType;
import io.apix.document.Connection;
import io.apix.document.ConnectionRepository;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Application settings page for global source connections.
 */
public class ConnectionsConfigurable implements SearchableConfigurable {

    private static final DocumentSourceType[] SUPPORTED_TYPES = {
            DocumentSourceType.OpenAPI,
            DocumentSourceType.SwaggerHub,
            DocumentSourceType.Apifox
    };
    private static DocumentSourceType typeToSelectOnOpen = DocumentSourceType.OpenAPI;

    private final ConnectionRepository repository = ConnectionRepository.getInstance();
    private final Map<DocumentSourceType, ConnectionPanel> panels = new EnumMap<>(DocumentSourceType.class);

    private JBTabbedPane tabs;
    private boolean modified;

    @Override
    public @NotNull String getId() {
        return "apix";
    }

    @Override
    public @Nls String getDisplayName() {
        return "Apix";
    }

    @Override
    public @Nullable JComponent createComponent() {
        tabs = new JBTabbedPane();
        for (DocumentSourceType type : SUPPORTED_TYPES) {
            ConnectionPanel panel = new ConnectionPanel(type);
            panels.put(type, panel);
            tabs.addTab(tabTitle(type), panel);
        }
        tabs.setSelectedIndex(tabIndex(typeToSelectOnOpen));
        typeToSelectOnOpen = DocumentSourceType.OpenAPI;
        reset();
        return tabs;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        for (ConnectionPanel panel : panels.values()) {
            panel.saveCurrentDetail();
        }

        Set<String> currentIds = new HashSet<>();
        for (DocumentSourceType type : SUPPORTED_TYPES) {
            ConnectionPanel panel = panels.get(type);
            for (Connection connection : panel.getConnections()) {
                if (StringUtils.isNotBlank(connection.getId())) {
                    currentIds.add(connection.getId());
                }
            }
        }

        for (Connection existing : repository.listSupported()) {
            if (!currentIds.contains(existing.getId())) {
                repository.delete(existing.getId());
            }
        }
        for (DocumentSourceType type : SUPPORTED_TYPES) {
            ConnectionPanel panel = panels.get(type);
            for (Connection connection : panel.getConnections()) {
                repository.save(connection);
            }
        }
        modified = false;
    }

    @Override
    public void reset() {
        if (tabs == null) {
            return;
        }
        for (DocumentSourceType type : SUPPORTED_TYPES) {
            ConnectionPanel panel = panels.get(type);
            panel.setConnections(new ArrayList<>(repository.list(type)));
        }
        modified = false;
    }

    @Override
    public void disposeUIResources() {
        panels.clear();
        tabs = null;
    }

    private void markModified() {
        modified = true;
    }

    private static String tabTitle(DocumentSourceType type) {
        if (type == DocumentSourceType.Apifox) {
            return "Apifox";
        }
        return type == DocumentSourceType.SwaggerHub ? "SwaggerHub" : "OpenAPI";
    }

    public static void selectTypeOnNextOpen(DocumentSourceType type) {
        typeToSelectOnOpen = type != null ? type : DocumentSourceType.OpenAPI;
    }

    private static int tabIndex(DocumentSourceType type) {
        if (type == DocumentSourceType.SwaggerHub) {
            return 1;
        }
        return type == DocumentSourceType.Apifox ? 2 : 0;
    }

    private static String defaultName(DocumentSourceType type) {
        if (type == DocumentSourceType.Apifox) {
            return "Apifox";
        }
        return type == DocumentSourceType.SwaggerHub ? "SwaggerHub" : "OpenAPI";
    }

    private static String defaultBaseUrl(DocumentSourceType type) {
        if (type == DocumentSourceType.Apifox) {
            return "https://api.apifox.com";
        }
        return type == DocumentSourceType.SwaggerHub ? "https://api.swaggerhub.com" : "";
    }

    private static String credentialLabel(DocumentSourceType type) {
        if (type == DocumentSourceType.Apifox) {
            return "Access Token";
        }
        return type == DocumentSourceType.SwaggerHub ? "API Key" : "Credential";
    }

    private class ConnectionPanel extends JPanel {

        private final DocumentSourceType type;
        private final List<Connection> connections = new ArrayList<>();
        private final DefaultListModel<Connection> listModel = new DefaultListModel<>();
        private final JBList<Connection> connectionList = new JBList<>(listModel);
        private final JButton addButton = new JButton("+ Add Connection");

        private final JPanel contentPanel = new JPanel(new CardLayout());
        private final JPanel editorPanel = new JPanel(new BorderLayout());
        private final JPanel rightPanel = new JPanel(new CardLayout());
        private final JPanel emptyPanel = new JPanel(new GridBagLayout());
        private final JPanel detailPanel = new JPanel(new GridBagLayout());
        private final JLabel credentialLabel = new JLabel();
        private JLabel baseUrlLabel;
        private JLabel authTypeLabel;
        private JLabel usernameLabel;
        private JLabel headerNameLabel;
        private final JBTextField nameField = new JBTextField();
        private final JBTextField baseUrlField = new JBTextField();
        private final JComboBox<AuthTypeItem> authTypeComboBox = new JComboBox<>();
        private final JBTextField usernameField = new JBTextField();
        private final JBTextField headerNameField = new JBTextField();
        private final JBPasswordField credentialField = new JBPasswordField();
        private final JToggleButton showCredentialButton = new JToggleButton("Show");
        private final JButton deleteButton = new JButton("Delete");

        private int selectedIndex = -1;
        private boolean loading;
        private char credentialEchoChar;

        ConnectionPanel(DocumentSourceType type) {
            super(new BorderLayout());
            this.type = type;
            setBorder(JBUI.Borders.empty(8, 0, 0, 0));
            buildHeader();
            buildListPanel();
            buildRightPanel();
            buildEmptyPanel();
            contentPanel.add(emptyPanel, "empty");
            contentPanel.add(editorPanel, "editor");
            add(contentPanel, BorderLayout.CENTER);
        }

        List<Connection> getConnections() {
            return connections;
        }

        void setConnections(List<Connection> values) {
            loading = true;
            connections.clear();
            connections.addAll(values);
            selectedIndex = connections.isEmpty() ? -1 : 0;
            refreshList();
            loading = false;
            loadDetail();
        }

        void saveCurrentDetail() {
            if (selectedIndex < 0 || selectedIndex >= connections.size()) {
                return;
            }
            Connection connection = connections.get(selectedIndex);
            connection.setType(type);
            connection.setName(nameField.getText().trim());
            connection.setBaseUrl(type == DocumentSourceType.OpenAPI ? null : baseUrlField.getText().trim());
            ConnectionAuthType authType = selectedAuthType();
            connection.setAuthType(authType);
            connection.setUsername(authType == ConnectionAuthType.Basic ? usernameField.getText().trim() : null);
            connection.setHeaderName(authType == ConnectionAuthType.Header ? headerNameField.getText().trim() : null);
            connection.setCredential(authType == ConnectionAuthType.None ? "" : new String(credentialField.getPassword()));
            if (connection.getCreatedAt() == null) {
                connection.setCreatedAt(System.currentTimeMillis());
            }
            connectionList.repaint();
        }

        private void buildHeader() {
            addButton.setFocusable(false);
            addButton.addActionListener(e -> addConnection());

            JPanel header = new JPanel(new BorderLayout());
            header.setBorder(JBUI.Borders.empty(0, 0, 8, 0));
            header.add(addButton, BorderLayout.WEST);
            add(header, BorderLayout.NORTH);
        }

        private void buildListPanel() {
            connectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            connectionList.setFocusable(false);
            connectionList.setCellRenderer(new ConnectionListRenderer());
            connectionList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || loading) {
                    return;
                }
                saveCurrentDetail();
                selectedIndex = connectionList.getSelectedIndex();
                loadDetail();
            });

            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setPreferredSize(new Dimension(JBUI.scale(250), JBUI.scale(360)));
            leftPanel.setBorder(JBUI.Borders.emptyRight(12));
            leftPanel.add(new JScrollPane(connectionList), BorderLayout.CENTER);
            editorPanel.add(leftPanel, BorderLayout.WEST);
        }

        private void buildRightPanel() {
            buildDetailPanel();
            rightPanel.add(detailPanel, "detail");
            editorPanel.add(rightPanel, BorderLayout.CENTER);
        }

        private void buildEmptyPanel() {
            JLabel label = new JLabel("No connections yet.");
            label.setForeground(UIManager.getColor("Label.disabledForeground"));
            emptyPanel.add(label);
        }

        private void buildDetailPanel() {
            credentialEchoChar = credentialField.getEchoChar();
            credentialField.setColumns(18);
            showCredentialButton.setFocusable(false);
            showCredentialButton.addActionListener(e -> {
                boolean showing = showCredentialButton.isSelected();
                credentialField.setEchoChar(showing ? (char) 0 : credentialEchoChar);
                showCredentialButton.setText(showing ? "Hide" : "Show");
            });
            deleteButton.setFocusable(false);
            deleteButton.addActionListener(e -> deleteSelectedConnection());

            authTypeComboBox.addItem(new AuthTypeItem(ConnectionAuthType.Bearer));
            authTypeComboBox.addItem(new AuthTypeItem(ConnectionAuthType.Basic));
            authTypeComboBox.addItem(new AuthTypeItem(ConnectionAuthType.Header));
            authTypeComboBox.addActionListener(e -> {
                updateAuthFields();
                if (!loading) {
                    saveCurrentDetail();
                    markModified();
                }
            });

            addChangeListener(nameField);
            addChangeListener(baseUrlField);
            addChangeListener(usernameField);
            addChangeListener(headerNameField);
            addChangeListener(credentialField);

            addRow(0, "Name", nameField);
            baseUrlLabel = addRow(2, "Base URL", baseUrlField);
            authTypeLabel = addRow(4, "Auth Type", authTypeComboBox);
            usernameLabel = addRow(6, "Username", usernameField);
            headerNameLabel = addRow(8, "Header Name", headerNameField);
            addCredentialRow(10);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 12;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.anchor = GridBagConstraints.SOUTHEAST;
            gbc.insets = new Insets(JBUI.scale(16), 0, 0, 0);
            detailPanel.add(deleteButton, gbc);
        }

        private JLabel addRow(int row, String label, JComponent field) {
            JLabel labelComponent = new JLabel(label);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, JBUI.scale(4), 0);
            detailPanel.add(labelComponent, gbc);

            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row + 1;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, JBUI.scale(12), 0);
            detailPanel.add(field, gbc);
            return labelComponent;
        }

        private void addCredentialRow(int row) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, JBUI.scale(4), 0);
            detailPanel.add(credentialLabel, gbc);

            JPanel rowPanel = new JPanel(new BorderLayout(JBUI.scale(6), 0));
            rowPanel.add(credentialField, BorderLayout.CENTER);
            rowPanel.add(showCredentialButton, BorderLayout.EAST);

            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row + 1;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, JBUI.scale(12), 0);
            detailPanel.add(rowPanel, gbc);
        }

        private void addConnection() {
            saveCurrentDetail();
            Connection connection = new Connection();
            connection.setType(type);
            connection.setName(defaultName(type));
            connection.setBaseUrl(type == DocumentSourceType.OpenAPI ? null : defaultBaseUrl(type));
            connection.setAuthType(defaultAuthType(type));
            connection.setCredential("");
            connection.setCreatedAt(System.currentTimeMillis());
            connections.add(connection);
            selectedIndex = connections.size() - 1;
            refreshList();
            loadDetail();
            nameField.requestFocusInWindow();
            nameField.selectAll();
            markModified();
        }

        private void deleteSelectedConnection() {
            if (selectedIndex < 0 || selectedIndex >= connections.size()) {
                return;
            }
            connections.remove(selectedIndex);
            selectedIndex = connections.isEmpty() ? -1 : Math.min(selectedIndex, connections.size() - 1);
            refreshList();
            loadDetail();
            markModified();
        }

        private void refreshList() {
            loading = true;
            listModel.clear();
            for (Connection connection : connections) {
                listModel.addElement(connection);
            }
            connectionList.setSelectedIndex(selectedIndex);
            loading = false;
        }

        private void loadDetail() {
            loading = true;
            if (selectedIndex < 0 || selectedIndex >= connections.size()) {
                nameField.setText("");
                baseUrlField.setText("");
                usernameField.setText("");
                headerNameField.setText("");
                credentialField.setText("");
                ((CardLayout) contentPanel.getLayout()).show(contentPanel, "empty");
                loading = false;
                return;
            }
            Connection connection = connections.get(selectedIndex);
            if (StringUtils.isNotBlank(connection.getId()) && connection.getCredential() == null) {
                Connection stored = repository.getWithCredential(connection.getId());
                if (stored != null) {
                    connection.setCredential(stored.getCredential());
                } else {
                    connection.setCredential("");
                }
            }
            credentialLabel.setText(credentialLabel(type));
            nameField.setText(connection.getName());
            baseUrlField.setText(connection.getBaseUrl());
            authTypeComboBox.setSelectedItem(new AuthTypeItem(defaultAuthType(connection.getType(), connection.getAuthType())));
            usernameField.setText(connection.getUsername());
            headerNameField.setText(connection.getHeaderName());
            credentialField.setText(StringUtils.defaultString(connection.getCredential()));
            credentialField.setEchoChar(showCredentialButton.isSelected() ? (char) 0 : credentialEchoChar);
            updateAuthFields();
            ((CardLayout) contentPanel.getLayout()).show(contentPanel, "editor");
            ((CardLayout) rightPanel.getLayout()).show(rightPanel, "detail");
            loading = false;
        }

        private void addChangeListener(JTextField field) {
            field.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { changed(); }
                public void removeUpdate(DocumentEvent e) { changed(); }
                public void changedUpdate(DocumentEvent e) { changed(); }

                private void changed() {
                    if (loading) {
                        return;
                    }
                    saveCurrentDetail();
                    markModified();
                }
            });
        }

        private class ConnectionListRenderer extends JPanel implements ListCellRenderer<Connection> {

            private final JLabel nameLabel = new JLabel();
            private final JLabel urlLabel = new JLabel();

            ConnectionListRenderer() {
                super(new BorderLayout());
                setBorder(JBUI.Borders.empty(7, 8));
                nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
                add(nameLabel, BorderLayout.NORTH);
                add(urlLabel, BorderLayout.SOUTH);
            }

            @Override
            public Component getListCellRendererComponent(
                    JList<? extends Connection> list,
                    Connection value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                nameLabel.setText(StringUtils.defaultIfBlank(value.getName(), "Untitled Connection"));
                urlLabel.setText(StringUtils.defaultIfBlank(value.getBaseUrl(), defaultBaseUrl(type)));
                if (type == DocumentSourceType.OpenAPI) {
                    urlLabel.setText(authLabel(value.getAuthType()));
                }
                Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
                Color fg = isSelected ? list.getSelectionForeground() : list.getForeground();
                Color disabled = UIManager.getColor("Label.disabledForeground");
                setBackground(bg);
                nameLabel.setForeground(fg);
                urlLabel.setForeground(isSelected ? fg : disabled);
                setOpaque(true);
                return this;
            }
        }

        private ConnectionAuthType selectedAuthType() {
            if (type == DocumentSourceType.Apifox) {
                return ConnectionAuthType.Bearer;
            }
            if (type == DocumentSourceType.SwaggerHub) {
                return ConnectionAuthType.Header;
            }
            Object selected = authTypeComboBox.getSelectedItem();
            return selected instanceof AuthTypeItem
                    ? ((AuthTypeItem) selected).authType
                    : ConnectionAuthType.None;
        }

        private void updateAuthFields() {
            ConnectionAuthType authType = selectedAuthType();
            boolean openApi = type == DocumentSourceType.OpenAPI;
            boolean basic = openApi && authType == ConnectionAuthType.Basic;
            boolean header = openApi && authType == ConnectionAuthType.Header;
            boolean credential = !openApi || authType != ConnectionAuthType.None;
            authTypeLabel.setVisible(openApi);
            authTypeComboBox.setVisible(type == DocumentSourceType.OpenAPI);
            baseUrlLabel.setVisible(!openApi);
            baseUrlField.setVisible(!openApi);
            usernameLabel.setVisible(basic);
            usernameField.setVisible(basic);
            headerNameLabel.setVisible(header);
            headerNameField.setVisible(header);
            credentialLabel.setVisible(credential);
            credentialField.setVisible(credential);
            showCredentialButton.setVisible(credential);
            if (type == DocumentSourceType.OpenAPI) {
                credentialLabel.setText(credentialLabel(authType));
            }
        }
    }

    private static ConnectionAuthType defaultAuthType(DocumentSourceType type) {
        return defaultAuthType(type, null);
    }

    private static ConnectionAuthType defaultAuthType(DocumentSourceType type, ConnectionAuthType authType) {
        if (authType != null) {
            return authType;
        }
        if (type == DocumentSourceType.Apifox) {
            return ConnectionAuthType.Bearer;
        }
        if (type == DocumentSourceType.SwaggerHub) {
            return ConnectionAuthType.Header;
        }
        return ConnectionAuthType.Basic;
    }

    private static String credentialLabel(ConnectionAuthType authType) {
        if (authType == ConnectionAuthType.Bearer) {
            return "Token";
        }
        if (authType == ConnectionAuthType.Basic) {
            return "Password";
        }
        if (authType == ConnectionAuthType.Header) {
            return "Header Value";
        }
        return "Credential";
    }

    private static String authLabel(ConnectionAuthType authType) {
        ConnectionAuthType value = authType != null ? authType : ConnectionAuthType.None;
        if (value == ConnectionAuthType.Bearer) {
            return "Bearer Token";
        }
        if (value == ConnectionAuthType.Basic) {
            return "Basic Auth";
        }
        if (value == ConnectionAuthType.Header) {
            return "Custom Header";
        }
        return "No auth";
    }

    private static class AuthTypeItem {
        private final ConnectionAuthType authType;

        AuthTypeItem(ConnectionAuthType authType) {
            this.authType = authType;
        }

        @Override
        public String toString() {
            return authLabel(authType);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AuthTypeItem && ((AuthTypeItem) obj).authType == authType;
        }

        @Override
        public int hashCode() {
            return authType.hashCode();
        }
    }
}
