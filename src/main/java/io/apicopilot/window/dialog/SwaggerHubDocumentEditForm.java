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
public class SwaggerHubDocumentEditForm implements DocumentEditForm {
    private JPanel panel;
    private JTextField nameField;
    private JTextField serviceUrlField;
    private JTextField apiKeyField;
    private JTextField ownerField;
    private JTextField apiField;
    private JTextField versionField;
    private Document document;

    @Override
    public void set(@NotNull Document data) {
        this.document = data;
        nameField.setText(data.getName());
        Document.SwaggerHubConfig swaggerHubConfig = data.getSwaggerHubConfig();
        if (swaggerHubConfig != null) {
            serviceUrlField.setText(swaggerHubConfig.getServiceUrl());
            apiKeyField.setText(swaggerHubConfig.getApiKey());
            ownerField.setText(swaggerHubConfig.getOwner());
            apiField.setText(swaggerHubConfig.getApi());
            versionField.setText(swaggerHubConfig.getVersion());
        }
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
        swaggerHubConfig.setServiceUrl(serviceUrlField.getText().trim());
        swaggerHubConfig.setApiKey(apiKeyField.getText().trim());
        swaggerHubConfig.setOwner(ownerField.getText().trim());
        swaggerHubConfig.setApi(apiField.getText().trim());
        swaggerHubConfig.setVersion(versionField.getText().trim());

        return data;
    }

    @Override
    public ValidationInfo validate() {
        Document data = get();
        if (StringUtils.isEmpty(data.getName())) {
            return new ValidationInfo("Name is required", nameField);
        }
        Document.SwaggerHubConfig swaggerHubConfig = data.getSwaggerHubConfig();
        if (StringUtils.isEmpty(swaggerHubConfig.getServiceUrl())) {
            return new ValidationInfo("Service URL is required", serviceUrlField);
        }
        if (StringUtils.isEmpty(swaggerHubConfig.getApiKey())) {
            return new ValidationInfo("Api Key is required", apiKeyField);
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
}
