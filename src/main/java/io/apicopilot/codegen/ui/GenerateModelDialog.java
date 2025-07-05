package io.apicopilot.codegen.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 生成模型代码对话框
 */
public class GenerateModelDialog extends DialogWrapper {

    private GenerateModelPanel contentPane;
    private final Project project;
    private final Document document;
    private final Request request;

    public GenerateModelDialog(@Nullable Project project,  Document document, Request request) {
        super(project);
        setTitle("Generate Model Code");
        this.project = project;
        this.document = document;
        this.request = request;
        init();
        Disposer.register(getDisposable(), contentPane);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        if(contentPane == null) {
            contentPane = new GenerateModelPanel(this.project, this.document, this.request);
            contentPane.setLanguage("TypeScript");
        }
        return contentPane;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[0];
    }
}
