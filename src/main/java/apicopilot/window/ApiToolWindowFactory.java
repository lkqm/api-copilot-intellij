package apicopilot.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ApiToolWindowFactory implements ToolWindowFactory {

    public static final String TOOL_WINDOW_ID = "apicopilot.ApiToolWindow";
    public static final String TOOL_WINDOW_TOOLBAR_ID = "apicopilot.ApiToolWindow.Toolbar";
    private static final String TOOL_WINDOW_TITLE = "API";

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setTitle(TOOL_WINDOW_TITLE);
        toolWindow.setStripeTitle(TOOL_WINDOW_TITLE);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(new ApiToolWindow(project), "", false);
        toolWindow.getContentManager().addContent(content);
    }

}