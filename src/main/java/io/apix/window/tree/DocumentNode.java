package io.apix.window.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.util.ui.ConfirmationDialog;
import io.apix.document.Document;
import io.apix.document.DocumentClipboard;
import io.apix.document.DocumentManager;
import io.apix.document.DocumentRepository;
import io.apix.document.LoadResult;
import io.apix.document.SyncStatus;
import io.apix.util.NotificationUtils;
import io.apix.window.ApiViewTreePane;
import io.apix.window.dialog.DocumentEditDialog;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Document node.
 */
public class DocumentNode extends ApiViewNode<DocumentNode.Context> {

    public DocumentNode(Icon icon, @NonNull DocumentNode.Context data) {
        super(icon, data);
    }

    @Override
    public JPopupMenu getPopupMenu(MouseEventContext ctx) {
        Project project = ctx.getProject();
        Document document = data.getDocument();
        DefaultActionGroup group = new DefaultActionGroup();

        // Reload document
        if (document.getSyncStatus() != SyncStatus.SYNCING) {
            group.add(new AnAction("Reload") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent actionEvent) {
                    document.setSyncStatus(SyncStatus.SYNCING);
                    data.getTreePane().refreshDocumentNode(document, false);

                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        DocumentManager documentManager = DocumentManager.getInstance(project);
                        LoadResult loadResult = documentManager.reloadDocument(document);
                        if (!loadResult.isSuccess()) {
                            NotificationUtils.notifyError("Refresh document failed", loadResult.getFailReason());
                        }
                    });
                }
            });
        }

        // Enable document
        boolean enable = document.isEnable();
        group.add(new AnAction(enable ? "Disable" : "Enable") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent actionEvent) {
                document.setEnable(!enable);
                DocumentRepository.getInstance(project).save(document);
                data.getTreePane().setDocumentNodeDisable(document.getId());
            }
        });

        // Edit document
        group.add(new AnAction("Edit") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent actionEvent) {
                DocumentEditDialog.show(project, document.getType(), document);
            }
        });

        // Delete document
        group.add(new AnAction("Delete") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent actionEvent) {
                DocumentRepository.getInstance(project).delete(document.getId());
                data.getTreePane().removeDocumentNode(document.getId());
            }
        });

        // Expand all children
        group.add(new AnAction("Expand All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent actionEvent) {
                data.getTreePane().expandDocumentNode(document.getId());
            }
        });

        // Duplicate document
        group.add(new AnAction("Duplicate") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent actionEvent) {
                DocumentEditDialog.showDuplicate(project, document);
            }
        });


        return ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.POPUP, group)
                .getComponent();
    }

    @Override
    public void keyPressed(KeyEventContext ctx) {
        KeyEvent event = ctx.getEvent();
        Project project = ctx.getProject();

        boolean isCtrl = event.isControlDown() || event.isMetaDown();

        // 移除
        if (event.getKeyCode() == KeyEvent.VK_DELETE) {
            String title = "Remove Document";
            String message = String.format("Remove document `%s` from this project?", data.getDocument().getName());
            ConfirmationDialog confirmationDialog = new ConfirmationDialog(project, message, title, AllIcons.General.QuestionDialog, VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION);
            confirmationDialog.show();
            if (confirmationDialog.isOK()) {
                DocumentRepository.getInstance(project).delete(data.getDocument().getId());
                data.getTreePane().removeDocumentNode(data.getDocument().getId());
            }
        }

        // Ctrl+C: copy document to clipboard
        if (isCtrl && event.getKeyCode() == KeyEvent.VK_C) {
            DocumentClipboard.copy(data.getDocument());
        }

        // Ctrl+V: paste document from clipboard
        if (isCtrl && event.getKeyCode() == KeyEvent.VK_V && DocumentClipboard.hasCopy()) {
            DocumentEditDialog.showDuplicate(project, DocumentClipboard.get());
        }
    }

    /**
     * Connection data.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Context {

        private ApiViewTreePane treePane;

        @NotNull
        private Project project;

        /**
         * 文档源
         */
        private Document document;

        /**
         * api counts
         */
        private int apiCounts;

    }
}
