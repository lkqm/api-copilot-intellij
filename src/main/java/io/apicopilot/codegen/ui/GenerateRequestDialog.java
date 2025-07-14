package io.apicopilot.codegen.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
            contentPane.setLanguage("C#");
        }
        return contentPane;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[0];
    }
}
