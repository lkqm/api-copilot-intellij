package io.apicopilot.codegen.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import io.apicopilot.codegen.core.GenerateConfigs;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.LanguageUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class GenerateCodeDialog extends DialogWrapper {

    private GenerateCodePanel contentPane;
    private final Project project;
    private final Document document;
    private final List<Request> requests;

    public GenerateCodeDialog(@Nullable Project project, Document document, List<Request> requests) {
        super(project);
        setTitle("Generate Code");
        this.project = project;
        this.document = document;
        this.requests = requests;
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        if (contentPane == null) {
            contentPane = new GenerateCodePanel(this.project, this.document, this.requests);
            List<String> languages = GenerateConfigs.getInstance().getConfig().getCodeLanguages();
            String language = LanguageUtils.getDefaultLanguage(languages, "TypeScript");
            contentPane.setLanguage(language);
        }
        return contentPane;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        return contentPane.doValidate();
    }

    @Override
    protected void doOKAction() {
        contentPane.generate();
        super.doOKAction();
    }
}
