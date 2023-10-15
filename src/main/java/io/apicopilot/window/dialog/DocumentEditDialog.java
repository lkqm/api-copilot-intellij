package io.apicopilot.window.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentManager;
import io.apicopilot.document.DocumentRepository;
import io.apicopilot.document.DocumentSourceType;
import io.apicopilot.document.topic.DocumentTopic;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Document edit dialog.
 */
public class DocumentEditDialog extends DialogWrapper {
    private final Project project;
    private final DocumentEditForm form;
    private final Document document;


    public static DocumentEditDialog show(Project project, DocumentSourceType type, Document document) {
        String titleTpl = document != null ? "Edit %s Document" : "Add %s Document";
        String title = String.format(titleTpl, type.name());
        DocumentEditForm form = DocumentSourceType.getDocumentEditForm(type);
        DocumentEditDialog dialog = new DocumentEditDialog(project, title, document, form);
        dialog.show();
        return dialog;
    }

    DocumentEditDialog(@Nullable Project project, String title, Document document, DocumentEditForm form) {
        super(project);
        this.project = project;
        this.document = document;
        this.form = form;
        setTitle(title);
        init();
    }

    @Override
    protected void init() {
        super.init();
        if (document != null) {
            form.set(document);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return form.getPanel();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        Document document = form.get();
        document.setLoading(true);
        DocumentRepository.getInstance(project).save(document);

        // publish document event
        DocumentTopic topic = project.getMessageBus().syncPublisher(DocumentTopic.TOPIC);
        if (this.document == null) {
            topic.onAdded(document);
        } else {
            topic.onModified(document);
        }

        // async load document
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            DocumentManager manager = DocumentManager.getInstance(project);
            manager.reloadDocument(document);
        });
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        return form.validate();
    }
}