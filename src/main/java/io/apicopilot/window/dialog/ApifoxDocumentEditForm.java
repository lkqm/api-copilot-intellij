package io.apicopilot.window.dialog;

import com.intellij.openapi.ui.ValidationInfo;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentSourceType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Document edit form.
 */
@Getter
public class ApifoxDocumentEditForm implements DocumentEditForm {
    private JPanel panel;
    private JTextField nameField;
    private JTextField serviceUrlField;
    private JTextField accessTokenField;
    private JTextField projectIdField;
    private Document document;

    @Override
    public void set(@NotNull Document data) {
        this.document = data;
        nameField.setText(data.getName());
        Document.ApifoxConfig apifoxConfig = data.getApifoxConfig();
        if (apifoxConfig != null) {
            serviceUrlField.setText(apifoxConfig.getServiceUrl());
            accessTokenField.setText(apifoxConfig.getAccessToken());
            projectIdField.setText(apifoxConfig.getProjectId());
        }
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
        apifoxConfig.setServiceUrl(serviceUrlField.getText().trim());
        apifoxConfig.setAccessToken(accessTokenField.getText().trim());
        apifoxConfig.setProjectId(projectIdField.getText().trim());

        return data;
    }

    @Override
    public ValidationInfo validate() {
        Document document = get();
        if (StringUtils.isEmpty(document.getName())) {
            return new ValidationInfo("Name is required", nameField);
        }
        Document.ApifoxConfig apifoxConfig = document.getApifoxConfig();
        if (StringUtils.isEmpty(apifoxConfig.getServiceUrl())) {
            return new ValidationInfo("Service URL is required", serviceUrlField);
        }
        if (StringUtils.isEmpty(apifoxConfig.getAccessToken())) {
            return new ValidationInfo("Access Token is required", accessTokenField);
        }
        if (StringUtils.isEmpty(apifoxConfig.getProjectId())) {
            return new ValidationInfo("Project ID is required", projectIdField);
        }
        return null;
    }
}
