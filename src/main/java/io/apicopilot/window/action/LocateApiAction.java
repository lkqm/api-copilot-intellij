package io.apicopilot.window.action;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import io.apicopilot.window.ApiView;
import io.apicopilot.window.ApiViewPanel;
import io.apicopilot.window.support.PreviewState;
import org.jetbrains.annotations.NotNull;

/**
 * Select API action.
 */
public class LocateApiAction extends AnAction {

    public LocateApiAction() {
        getTemplatePresentation().setText("Locate API");
        getTemplatePresentation().setIcon(AllIcons.General.Locate);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ApiView apiView = ApiView.getInstance(project);
        apiView.locate();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ApiViewPanel panel = ApiView.getInstance(project).getPanel();
        boolean canLocate = panel.getPreviewState() != PreviewState.HIDDEN && panel.getPreviewPane().getDocument() != null && panel.getPreviewPane().getRequest() != null;
        e.getPresentation().setEnabled(canLocate);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
