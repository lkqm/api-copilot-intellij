package io.apix.window;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Tool window factory for API view.
 */
public class ApiViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    public static final String ID = "ApiView";
    private static final String TOOL_WINDOW_TITLE = "APIs";

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setTitle(TOOL_WINDOW_TITLE);
        toolWindow.setStripeTitle(TOOL_WINDOW_TITLE);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow window) {
        ApiView.getInstance(project).setup(window);
    }

}