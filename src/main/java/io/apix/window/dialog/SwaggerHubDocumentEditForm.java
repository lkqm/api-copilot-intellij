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
public class SwaggerHubDocumentEditForm implements DocumentEditForm {
    private JPanel panel;
    private JTextField nameField;
    private JLabel serviceUrlLabel;
    private JTextField serviceUrlField;
    private JLabel apiKeyLabel;
    private JTextField apiKeyField;
    private JLabel connectionLabel;
    private JPanel connectionPanel;
    private JComboBox<ConnectionItem> connectionComboBox;
    private JButton manageConnectionsButton;
    private JTextField ownerField;
    private JTextField apiField;
    private JTextField versionField;
    private JCheckBox autoSyncEnabledCheckBox;
    private JCheckBox exportEnabledCheckBox;
    private JLabel autoSyncHintLabel;
    private Document document;

    public SwaggerHubDocumentEditForm() {
        connectionComboBox.setPrototypeDisplayValue(ConnectionItem.widthPrototype());
        manageConnectionsButton.addActionListener(e -> {
            ConnectionsConfigurable.selectTypeOnNextOpen(DocumentSourceType.SwaggerHub);
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
        Document.SwaggerHubConfig swaggerHubConfig = data.getSwaggerHubConfig();
        if (swaggerHubConfig != null) {
            serviceUrlField.setText(swaggerHubConfig.getServiceUrl());
            apiKeyField.setText(swaggerHubConfig.getApiKey());
            reloadConnections(swaggerHubConfig.getConnectionId(), hasLegacyCredential(swaggerHubConfig));
            setLegacyFieldsVisible(StringUtils.isBlank(swaggerHubConfig.getConnectionId()) && hasLegacyCredential(swaggerHubConfig));
            ownerField.setText(swaggerHubConfig.getOwner());
            apiField.setText(swaggerHubConfig.getApi());
            versionField.setText(swaggerHubConfig.getVersion());
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
            data.setType(DocumentSourceType.SwaggerHub);
        }

        // Name
        data.setName(nameField.getText().trim());

        Document.SwaggerHubConfig swaggerHubConfig = data.getSwaggerHubConfig();
        if (swaggerHubConfig == null) {
            swaggerHubConfig = new Document.SwaggerHubConfig();
            data.setSwaggerHubConfig(swaggerHubConfig);
        }
        swaggerHubConfig.setConnectionId(getSelectedConnectionId());
        swaggerHubConfig.setServiceUrl(serviceUrlField.getText().trim());
        swaggerHubConfig.setApiKey(apiKeyField.getText().trim());
        swaggerHubConfig.setOwner(ownerField.getText().trim());
        swaggerHubConfig.setApi(apiField.getText().trim());
        swaggerHubConfig.setVersion(versionField.getText().trim());
        data.setAutoSyncEnabled(document == null || autoSyncEnabledCheckBox.isSelected());
        data.setExportEnabled(exportEnabledCheckBox.isSelected());

        return data;
    }

    @Override
    public ValidationInfo validate() {
        Document data = get();
        if (StringUtils.isEmpty(data.getName())) {
            return new ValidationInfo("Name is required", nameField);
        }
        Document.SwaggerHubConfig swaggerHubConfig = data.getSwaggerHubConfig();
        if (StringUtils.isEmpty(swaggerHubConfig.getConnectionId()) && !hasLegacyCredential(swaggerHubConfig)) {
            return new ValidationInfo("Connection is required", connectionComboBox);
        }
        if (StringUtils.isEmpty(swaggerHubConfig.getOwner())) {
            return new ValidationInfo("Owner is required", ownerField);
        }
        if (StringUtils.isEmpty(swaggerHubConfig.getApi())) {
            return new ValidationInfo("Api ID is required", apiField);
        }
        if (StringUtils.isEmpty(swaggerHubConfig.getVersion())) {
            return new ValidationInfo("Api Version is required", versionField);
        }
        return null;
    }

    private void reloadConnections(String preferredId) {
        reloadConnections(preferredId, false);
    }

    private void reloadConnections(String preferredId, boolean keepEmptySelection) {
        connectionComboBox.removeAllItems();
        List<Connection> connections = ConnectionRepository.getInstance().list(DocumentSourceType.SwaggerHub);
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

    private boolean hasLegacyCredential(Document.SwaggerHubConfig config) {
        return config != null
                && StringUtils.isNotEmpty(config.getServiceUrl())
                && StringUtils.isNotEmpty(config.getApiKey());
    }

    private void setLegacyFieldsVisible(boolean visible) {
        serviceUrlLabel.setVisible(visible);
        serviceUrlField.setVisible(visible);
        apiKeyLabel.setVisible(visible);
        apiKeyField.setVisible(visible);
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
