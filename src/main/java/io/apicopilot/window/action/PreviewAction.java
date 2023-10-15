package io.apicopilot.window.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import io.apicopilot.window.ApiView;
import org.jetbrains.annotations.NotNull;

import static com.intellij.icons.AllIcons.General.LayoutEditorPreview;

/**
 * Preview request detail.
 */
public class PreviewAction extends AnAction {


    public PreviewAction() {
        getTemplatePresentation().setText("Preview");
        getTemplatePresentation().setIcon(LayoutEditorPreview);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        ApiView.getInstance(project).switchPreviewState();
    }


}
