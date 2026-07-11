package io.apix.codegen.ui;

import io.apix.codegen.CodegenLanguageResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import io.apix.codegen.core.GenerateConfigs;
import io.apix.codegen.model.GenerateModelTarget;
import io.apix.document.Document;
import io.apix.model.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * 生成模型代码对话框
 */
public class GenerateModelDialog extends DialogWrapper {

    private GenerateModelPanel contentPane;
    private final Project project;
    private final Document document;
    private final Request request;
    private final GenerateModelTarget target;

    public GenerateModelDialog(@Nullable Project project, Document document, Request request) {
        this(project, document, request, GenerateModelTarget.all());
    }

    public GenerateModelDialog(@Nullable Project project, Document document, Request request, GenerateModelTarget target) {
        super(project);
        setTitle("Generate Model Code");
        this.project = project;
        this.document = document;
        this.request = request;
        this.target = target != null ? target : GenerateModelTarget.all();
        init();
        Disposer.register(getDisposable(), contentPane);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        if (contentPane == null) {
            contentPane = new GenerateModelPanel(this.project, this.document, this.request, this.target);
            List<String> languages = GenerateConfigs.getInstance().getConfig().getModelLanguages();
            String language = CodegenLanguageResolver.resolveModelLanguage(project, languages);
            contentPane.setLanguage(language);
        }
        return contentPane;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[0];
    }
}
