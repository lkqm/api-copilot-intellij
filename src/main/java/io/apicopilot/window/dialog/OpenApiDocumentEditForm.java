package io.apicopilot.window.dialog;

import com.intellij.openapi.ui.ValidationInfo;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentSourceType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Document edit form.
 */
@Getter
public class OpenApiDocumentEditForm implements DocumentEditForm {
    private JTextField openApiFileField;
    private JPanel panel;
    private JTextField nameField;
    private Document document;

    @Override
    public void set(@NotNull Document data) {
        this.document = data;
        nameField.setText(data.getName());
        Document.OpenApiConfig config = data.getOpenApiConfig();
        if (config != null) {
            openApiFileField.setText(config.getPath());
        }
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
        data.getOpenApiConfig().setPath(openApiFileField.getText());
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
}
