package io.apix.window.dialog;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.ValidationInfo;
import io.apix.document.Connection;
import io.apix.document.ConnectionRepository;
import io.apix.document.Document;
import io.apix.document.DocumentSourceType;
import io.apix.window.settings.ConnectionsConfigurable;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.List;

/**
 * Document edit form.
 */
@Getter
public class ApifoxDocumentEditForm implements DocumentEditForm {
    private JPanel panel;
    private JTextField nameField;
    private JLabel serviceUrlLabel;
    private JTextField serviceUrlField;
    private JLabel accessTokenLabel;
    private JTextField accessTokenField;
    private JLabel connectionLabel;
    private JPanel connectionPanel;
    private JComboBox<ConnectionItem> connectionComboBox;
    private JButton manageConnectionsButton;
    private JTextField projectIdField;
    private JCheckBox autoSyncEnabledCheckBox;
    private JCheckBox exportEnabledCheckBox;
    private JLabel autoSyncHintLabel;
    private Document document;

    public ApifoxDocumentEditForm() {
        connectionComboBox.setPrototypeDisplayValue(ConnectionItem.widthPrototype());
        manageConnectionsButton.addActionListener(e -> {
            ConnectionsConfigurable.selectTypeOnNextOpen(DocumentSourceType.Apifox, true);
            ShowSettingsUtil.getInstance().showSettingsDialog(null, ConnectionsConfigurable.class);
            reloadConnections(getSelectedConnectionId());
        });
        reloadConnections(null);
        setLegacyFieldsVisible(false);
    }

    @Override
    public void set(@NotNull Document data) {
        this.document = data;
        nameField.setText(data.getName());
        Document.ApifoxConfig apifoxConfig = data.getApifoxConfig();
        if (apifoxConfig != null) {
            serviceUrlField.setText(apifoxConfig.getServiceUrl());
            accessTokenField.setText(apifoxConfig.getAccessToken());
            reloadConnections(apifoxConfig.getConnectionId(), hasLegacyCredential(apifoxConfig));
            setLegacyFieldsVisible(StringUtils.isBlank(apifoxConfig.getConnectionId()) && hasLegacyCredential(apifoxConfig));
            projectIdField.setText(apifoxConfig.getProjectId());
        } else {
            setLegacyFieldsVisible(false);
        }
        autoSyncEnabledCheckBox.setSelected(data.isAutoSyncEnabled());
        exportEnabledCheckBox.setSelected(data.isExportEnabled());
    }

    @Override
    @NotNull
    public Document get() {
        Document data = this.document;
        if (data == null) {
            data = new Document();
            data.setType(DocumentSourceType.Apifox);
        }

        // Name
        data.setName(nameField.getText().trim());

        Document.ApifoxConfig apifoxConfig = data.getApifoxConfig();
        if (apifoxConfig == null) {
            apifoxConfig = new Document.ApifoxConfig();
            data.setApifoxConfig(apifoxConfig);
        }
        apifoxConfig.setConnectionId(getSelectedConnectionId());
        apifoxConfig.setServiceUrl(serviceUrlField.getText().trim());
        apifoxConfig.setAccessToken(accessTokenField.getText().trim());
        apifoxConfig.setProjectId(projectIdField.getText().trim());
        data.setAutoSyncEnabled(document == null || autoSyncEnabledCheckBox.isSelected());
        data.setExportEnabled(exportEnabledCheckBox.isSelected());

        return data;
    }

    @Override
    public ValidationInfo validate() {
        Document document = get();
        if (StringUtils.isEmpty(document.getName())) {
            return new ValidationInfo("Name is required", nameField);
        }
        Document.ApifoxConfig apifoxConfig = document.getApifoxConfig();
        if (StringUtils.isEmpty(apifoxConfig.getConnectionId()) && !hasLegacyCredential(apifoxConfig)) {
            return new ValidationInfo("Connection is required", connectionComboBox);
        }
        if (StringUtils.isEmpty(apifoxConfig.getProjectId())) {
            return new ValidationInfo("Project ID is required", projectIdField);
        }
        return null;
    }

    private void reloadConnections(String preferredId) {
        reloadConnections(preferredId, false);
    }

    private void reloadConnections(String preferredId, boolean keepEmptySelection) {
        connectionComboBox.removeAllItems();
        List<Connection> connections = ConnectionRepository.getInstance().list(DocumentSourceType.Apifox);
        if (connections.isEmpty()) {
            connectionComboBox.addItem(ConnectionItem.empty());
            connectionComboBox.setSelectedIndex(0);
            return;
        }
        int selectedIndex = 0;
        boolean preferredFound = false;
        if (keepEmptySelection && StringUtils.isBlank(preferredId)) {
            connectionComboBox.addItem(ConnectionItem.unselected());
        }
        for (Connection connection : connections) {
            connectionComboBox.addItem(new ConnectionItem(connection));
            if (StringUtils.equals(preferredId, connection.getId())) {
                selectedIndex = connectionComboBox.getItemCount() - 1;
                preferredFound = true;
            }
        }
        if (StringUtils.isNotBlank(preferredId) && !preferredFound) {
            connectionComboBox.addItem(ConnectionItem.missing(preferredId));
            selectedIndex = connectionComboBox.getItemCount() - 1;
        }
        connectionComboBox.setSelectedIndex(selectedIndex);
    }

    private boolean hasLegacyCredential(Document.ApifoxConfig config) {
        return config != null
                && StringUtils.isNotEmpty(config.getServiceUrl())
                && StringUtils.isNotEmpty(config.getAccessToken());
    }

    private void setLegacyFieldsVisible(boolean visible) {
        serviceUrlLabel.setVisible(visible);
        serviceUrlField.setVisible(visible);
        accessTokenLabel.setVisible(visible);
        accessTokenField.setVisible(visible);
        connectionLabel.setVisible(!visible);
        connectionPanel.setVisible(!visible);
    }

    private String getSelectedConnectionId() {
        Object selected = connectionComboBox.getSelectedItem();
        if (selected instanceof ConnectionItem) {
            return ((ConnectionItem) selected).getId();
        }
        return "";
    }

    private static class ConnectionItem {
        private final String id;
        private final String name;

        ConnectionItem(Connection connection) {
            this.id = connection.getId();
            this.name = StringUtils.defaultIfBlank(connection.getName(), connection.getBaseUrl());
        }

        static ConnectionItem empty() {
            return new ConnectionItem("", "No connection");
        }

        static ConnectionItem unselected() {
            return new ConnectionItem("", "No connection");
        }

        static ConnectionItem missing(String id) {
            return new ConnectionItem(id, "Missing connection");
        }

        static ConnectionItem widthPrototype() {
            return new ConnectionItem("", "No connection");
        }

        private ConnectionItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        String getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
