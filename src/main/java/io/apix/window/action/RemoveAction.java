package io.apix.window.action;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.util.ui.ConfirmationDialog;
import io.apix.document.Document;
import io.apix.window.ApiView;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Remove documentation action.
 */
public class RemoveAction extends AnAction {

    public RemoveAction() {
        getTemplatePresentation().setText("Remove Document");
        getTemplatePresentation().setIcon(AllIcons.General.Remove);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        ApiView apiView = ApiView.getInstance(project);
        List<Document> documents = apiView.getSelectedDocuments();
        if (documents.isEmpty()) {
            return;
        }

        // confirm dialog
        String title = "Remove Document";
        String name = documents.stream().map(Document::getName).collect(Collectors.joining(" , "));
        String message = String.format("Remove document `%s` from this project?", name);
        ConfirmationDialog confirmationDialog = new ConfirmationDialog(project, message, title, AllIcons.General.QuestionDialog, VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION);
        confirmationDialog.show();
        if (!confirmationDialog.isOK()) {
            return;
        }

        // delete documents
        apiView.deleteDocuments(documents);
    }

}
