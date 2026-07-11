package io.apix.window.action;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import io.apix.document.DocumentSourceType;
import io.apix.window.dialog.DocumentEditDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Add documentation action.
 */
public class AddAction extends ActionGroup {

    public AddAction() {
        super("", true);
        getTemplatePresentation().setText("Add Document");
        getTemplatePresentation().setIcon(AllIcons.General.Add);
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent anActionEvent) {
        return new AnAction[]{
                new AnAction(DocumentSourceType.OpenAPI.name()) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
                        DocumentEditDialog.show(project, DocumentSourceType.OpenAPI, null);
                    }
                },
                new AnAction(DocumentSourceType.SwaggerHub.name()) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
                        DocumentEditDialog.show(project, DocumentSourceType.SwaggerHub, null);
                    }
                },
                new AnAction(DocumentSourceType.Apifox.name()) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
                        DocumentEditDialog.show(project, DocumentSourceType.Apifox, null);
                    }
                },
        };
    }

}
