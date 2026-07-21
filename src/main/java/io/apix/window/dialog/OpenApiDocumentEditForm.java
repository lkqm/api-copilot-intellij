package io.apix.window.dialog;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.ValidationInfo;
import io.apix.document.Connection;
import io.apix.document.ConnectionAuthType;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;

/**
 * Document edit form.
 */
@Getter
public class OpenApiDocumentEditForm implements DocumentEditForm {
    private JTextField openApiFileField;
    private JPanel panel;
    private JTextField nameField;
    private JLabel authLabel;
    private JPanel authPanel;
    private JComboBox<ConnectionItem> authComboBox;
    private JButton manageAuthButton;
    private JCheckBox autoSyncEnabledCheckBox;
    private JCheckBox exportEnabledCheckBox;
    private JLabel autoSyncHintLabel;
    private Document document;

    public OpenApiDocumentEditForm() {
        authComboBox.setPrototypeDisplayValue(ConnectionItem.widthPrototype());
        authPanel.setPreferredSize(new Dimension(
                openApiFileField.getPreferredSize().width,
                authPanel.getPreferredSize().height));
        manageAuthButton.addActionListener(e -> {
            ConnectionsConfigurable.selectTypeOnNextOpen(DocumentSourceType.OpenAPI);
            ShowSettingsUtil.getInstance().showSettingsDialog(null, ConnectionsConfigurable.class);
            reloadAuth(getSelectedConnectionId());
        });
        openApiFileField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateRemoteFieldsVisibility();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateRemoteFieldsVisibility();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateRemoteFieldsVisibility();
            }
        });
        reloadAuth(null);
        updateRemoteFieldsVisibility();
    }

    @Override
    public void set(@NotNull Document data) {
        this.document = data;
        nameField.setText(data.getName());
        Document.OpenApiConfig config = data.getOpenApiConfig();
        if (config != null) {
            openApiFileField.setText(config.getPath());
            reloadAuth(config.getConnectionId());
        }
        autoSyncEnabledCheckBox.setSelected(data.isAutoSyncEnabled());
        exportEnabledCheckBox.setSelected(data.isExportEnabled());
        updateRemoteFieldsVisibility();
    }

    @Override
    @NotNull
    public Document get() {
        Document data = this.document;
        if (data == null) {
            data = new Document();
            data.setType(DocumentSourceType.OpenAPI);
        }

        data.setName(nameField.getText().trim());
        if (data.getOpenApiConfig() == null) {
            data.setOpenApiConfig(new Document.OpenApiConfig());
        }
        String path = openApiFileField.getText().trim();
        data.getOpenApiConfig().setPath(path);
        boolean remote = isRemotePath(path);
        data.getOpenApiConfig().setConnectionId(remote ? getSelectedConnectionId() : null);
        if (document == null) {
            data.setAutoSyncEnabled(remote);
        } else if (!remote) {
            data.setAutoSyncEnabled(false);
        } else {
            data.setAutoSyncEnabled(autoSyncEnabledCheckBox.isSelected());
        }
        data.setExportEnabled(exportEnabledCheckBox.isSelected());
        return data;
    }

    @Override
    public ValidationInfo validate() {
        Document data = get();
        if (StringUtils.isEmpty(data.getName())) {
            return new ValidationInfo("Name is required", nameField);
        }
        if (StringUtils.isEmpty(data.getOpenApiConfig().getPath())) {
            return new ValidationInfo("OpenAPI file is required", openApiFileField);
        }
        if (isRemotePath(data.getOpenApiConfig().getPath()) && isSelectedConnectionMissing()) {
            return new ValidationInfo("Auth is unavailable", authComboBox);
        }
        return null;
    }

    private void updateRemoteFieldsVisibility() {
        boolean remote = isRemotePath(openApiFileField.getText());
        authLabel.setVisible(remote);
        authPanel.setVisible(remote);
        autoSyncEnabledCheckBox.setVisible(remote);
        autoSyncHintLabel.setVisible(remote);
        if (remote) {
            if (document == null) {
                autoSyncEnabledCheckBox.setSelected(true);
            }
        } else {
            autoSyncEnabledCheckBox.setSelected(false);
        }
    }

    private void reloadAuth(String preferredId) {
        authComboBox.removeAllItems();
        authComboBox.addItem(ConnectionItem.noAuth());
        int selectedIndex = 0;
        boolean preferredFound = false;
        for (Connection connection : ConnectionRepository.getInstance().list(DocumentSourceType.OpenAPI)) {
            authComboBox.addItem(new ConnectionItem(connection));
            if (StringUtils.equals(preferredId, connection.getId())) {
                selectedIndex = authComboBox.getItemCount() - 1;
                preferredFound = true;
            }
        }
        if (StringUtils.isNotBlank(preferredId) && !preferredFound) {
            authComboBox.addItem(ConnectionItem.unavailable(preferredId));
            selectedIndex = authComboBox.getItemCount() - 1;
        }
        authComboBox.setSelectedIndex(selectedIndex);
    }

    private String getSelectedConnectionId() {
        Object selected = authComboBox.getSelectedItem();
        if (selected instanceof ConnectionItem) {
            return ((ConnectionItem) selected).id;
        }
        return "";
    }

    private boolean isSelectedConnectionMissing() {
        Object selected = authComboBox.getSelectedItem();
        return selected instanceof ConnectionItem && ((ConnectionItem) selected).missing;
    }

    private boolean isRemotePath(String path) {
        String trimmedPath = path == null ? "" : path.trim();
        return trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://");
    }

    private static class ConnectionItem {
        private final String id;
        private final String name;
        private final boolean missing;

        ConnectionItem(Connection connection) {
            this.id = connection.getId();
            this.name = StringUtils.defaultIfBlank(connection.getName(), connection.getBaseUrl())
                    + " (" + authLabel(connection.getAuthType()) + ")";
            this.missing = false;
        }

        static ConnectionItem noAuth() {
            return new ConnectionItem("", "No auth", false);
        }

        static ConnectionItem unavailable(String id) {
            return new ConnectionItem(id, "Auth unavailable", true);
        }

        static ConnectionItem widthPrototype() {
            return new ConnectionItem("", "No auth", false);
        }

        private ConnectionItem(String id, String name, boolean missing) {
            this.id = id;
            this.name = name;
            this.missing = missing;
        }

        @Override
        public String toString() {
            return name;
        }
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
}
