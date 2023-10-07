package apicopilot.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ApiToolWindowManager {

    private static final ApiToolWindowManager INSTANCE = new ApiToolWindowManager();

    public static ApiToolWindowManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get ApiToolWindow instance
     */
    public ApiToolWindow getToolWindow(@NotNull Project project) {
        ToolWindow toolWindow = getWindow(project);
        if (toolWindow != null) {
            for (Component component : toolWindow.getComponent().getComponents()) {
                if (component instanceof ApiToolWindow) {
                    return ((ApiToolWindow) component);
                }
            }
        }
        return null;
    }


    /**
     * Get tool window.
     */
    public ToolWindow getWindow(@NotNull Project project) {
        return ToolWindowManager.getInstance(project).getToolWindow(ApiToolWindowFactory.TOOL_WINDOW_ID);
    }

    /**
     * Show tool window.
     */
    public boolean showWindow(@NotNull Project project, Runnable runnable) {
        ToolWindow window = getWindow(project);
        if (window == null) {
            return false;
        }
        window.show(runnable);
        return true;
    }

    /**
     * Hide tool window.
     */
    public boolean hideWindow(@NotNull Project project) {
        ToolWindow window = getWindow(project);
        if (window == null) {
            return false;
        }
        window.hide(null);
        return true;
    }
}
