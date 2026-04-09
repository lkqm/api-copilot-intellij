package io.apicopilot.window.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import io.apicopilot.document.*;
import io.apicopilot.document.topic.DocumentTopic;
import io.apicopilot.util.NotificationUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Document edit dialog.
 */
public class DocumentEditDialog extends DialogWrapper {
    private final Project project;
    private final DocumentEditForm form;
    private final Document document;
    private final boolean isDuplicate;


    public static DocumentEditDialog show(Project project, DocumentSourceType type, Document document) {
        String titleTpl = document != null ? "Edit %s Document" : "Add %s Document";
        String title = String.format(titleTpl, type.name());
        DocumentEditForm form = DocumentSourceType.getDocumentEditForm(type);
        DocumentEditDialog dialog = new DocumentEditDialog(project, title, document, form, false);
        dialog.show();
        return dialog;
    }

    public static DocumentEditDialog showDuplicate(Project project, Document original) {
        String title = String.format("Duplicate %s Document", original.getType().name());
        DocumentEditForm form = DocumentSourceType.getDocumentEditForm(original.getType());
        Document copy = original.duplicate();
        DocumentEditDialog dialog = new DocumentEditDialog(project, title, copy, form, true);
        dialog.show();
        return dialog;
    }

    DocumentEditDialog(@Nullable Project project, String title, Document document, DocumentEditForm form, boolean isDuplicate) {
        super(project);
        this.project = project;
        this.document = document;
        this.form = form;
        this.isDuplicate = isDuplicate;
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
        document.setSyncStatus(SyncStatus.SYNCING);
        DocumentRepository.getInstance(project).save(document);

        // publish document event
        DocumentTopic topic = project.getMessageBus().syncPublisher(DocumentTopic.TOPIC);
        if (this.document == null || this.isDuplicate) {
            topic.onAdded(document);
        } else {
            topic.onModified(document);
        }

        // async load document
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            DocumentManager manager = DocumentManager.getInstance(project);
            LoadResult loadResult = manager.reloadDocument(document);
            if (!loadResult.isSuccess()) {
                NotificationUtils.notifyError("Load document failed", loadResult.getFailReason());
            }
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
