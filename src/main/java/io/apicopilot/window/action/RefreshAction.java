package io.apicopilot.window.action;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import io.apicopilot.window.ApiView;
import org.jetbrains.annotations.NotNull;

/**
 * Refresh all documents action.
 */
public class RefreshAction extends AnAction {

    public RefreshAction() {
        getTemplatePresentation().setText("Refresh All");
        getTemplatePresentation().setIcon(AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        ApiView.getInstance(project).refresh();
    }
}
