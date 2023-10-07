package apicopilot.window.toolbar;

import apicopilot.window.ApiToolWindowFactory;
import com.intellij.openapi.actionSystem.*;
import lombok.experimental.UtilityClass;

/**
 * 窗口工具栏
 */
@UtilityClass
public class ApiToolWindowToolbar {

    public static ActionToolbar getToolbar() {
        AnAction action = getToolbarAction();
        return ActionManager.getInstance().createActionToolbar(
                ActionPlaces.TOOLBAR,
                action instanceof ActionGroup ? ((ActionGroup) action) : new DefaultActionGroup(),
                true
        );
    }

    public static AnAction getToolbarAction() {
        return ActionManager.getInstance().getAction(ApiToolWindowFactory.TOOL_WINDOW_TOOLBAR_ID);
    }

}
