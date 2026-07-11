package io.apix.window.dialog;

import com.intellij.openapi.ui.ValidationInfo;
import io.apix.document.Document;
import io.apix.document.DocumentSourceType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Document edit form.
 */
@Getter
public class OpenApiDocumentEditForm implements DocumentEditForm {
    private JTextField openApiFileField;
    private JPanel panel;
    private JTextField nameField;
    private JCheckBox autoSyncEnabledCheckBox;
    private JCheckBox exportEnabledCheckBox;
    private JLabel autoSyncHintLabel;
    private Document document;

    public OpenApiDocumentEditForm() {
        openApiFileField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateAutoSyncVisibility();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateAutoSyncVisibility();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateAutoSyncVisibility();
            }
        });
        updateAutoSyncVisibility();
    }

    @Override
    public void set(@NotNull Document data) {
        this.document = data;
        nameField.setText(data.getName());
        Document.OpenApiConfig config = data.getOpenApiConfig();
        if (config != null) {
            openApiFileField.setText(config.getPath());
        }
        autoSyncEnabledCheckBox.setSelected(data.isAutoSyncEnabled());
        exportEnabledCheckBox.setSelected(data.isExportEnabled());
        updateAutoSyncVisibility();
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
        return null;
    }

    private void updateAutoSyncVisibility() {
        boolean remote = isRemotePath(openApiFileField.getText());
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

    private boolean isRemotePath(String path) {
        String trimmedPath = path == null ? "" : path.trim();
        return trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://");
    }
}
