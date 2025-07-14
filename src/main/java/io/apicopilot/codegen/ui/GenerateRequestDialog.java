package io.apicopilot.codegen.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import io.apicopilot.codegen.core.GenerateConfigs;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.LanguageUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class GenerateRequestDialog extends DialogWrapper {

    private GenerateRequestPanel contentPane;
    private final Project project;
    private final Document document;
    private final Request request;

    public GenerateRequestDialog(@Nullable Project project, Document document, Request request) {
        super(project);
        setTitle("Generate Request Code");
        this.project = project;
        this.document = document;
        this.request = request;
        init();
        Disposer.register(getDisposable(), contentPane);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        if (contentPane == null) {
            contentPane = new GenerateRequestPanel(this.project, this.document, this.request);
            List<String> languages = GenerateConfigs.getInstance().getConfig().getRequestLanguages();
            String language = LanguageUtils.getDefaultLanguage(languages, "C#");
            if (language.equals("TypeScript")) {
                language = "JavaScript";
            }
            contentPane.setLanguage(language);
        }
        return contentPane;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[0];
    }
}
