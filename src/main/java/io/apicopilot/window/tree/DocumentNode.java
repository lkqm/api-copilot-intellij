package io.apicopilot.window.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.util.ui.ConfirmationDialog;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentManager;
import io.apicopilot.document.DocumentRepository;
import io.apicopilot.window.ApiViewTreePane;
import io.apicopilot.window.dialog.DocumentEditDialog;
import lombok.*;

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
        JPopupMenu menu = new JBPopupMenu();

        // Reload document
        if(!document.isLoading()) {
            JMenuItem reloadItem = new JBMenuItem("Reload");
            reloadItem.addActionListener(actionEvent -> {
                document.setLoading(true);
                data.getTreePane().refreshDocumentNode(document, false);

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    DocumentManager.getInstance(project).reloadDocument(document);
                });
            });
            menu.add(reloadItem);
        }

        // Enable document
        boolean enable = document.isEnable();
        JMenuItem switchItem = new JBMenuItem(enable ? "Disable" : "Enable");
        switchItem.addActionListener(actionEvent -> {
            document.setEnable(!enable);
            DocumentRepository.getInstance(project).save(document);
            data.getTreePane().setDocumentNodeDisable(document.getId());
        });
        menu.add(switchItem);

        // Edit document
        JMenuItem editItem = new JBMenuItem("Edit");
        editItem.addActionListener(actionEvent -> {
            DocumentEditDialog.show(project, document.getType(), document);
        });
        menu.add(editItem);

        // Delete document
        JMenuItem deleteItem = new JBMenuItem("Delete");
        deleteItem.addActionListener(actionEvent -> {
            DocumentRepository.getInstance(project).delete(document.getId());
            data.getTreePane().removeDocumentNode(document.getId());
        });

        menu.add(deleteItem);
        return menu;
    }

    @Override
    public void keyPressed(KeyEventContext ctx) {
        KeyEvent event = ctx.getEvent();
        Project project = ctx.getProject();

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
