package apicopilot.search;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoClassPresentationUpdater;
import com.intellij.ide.actions.SearchEverywhereBaseAction;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class GotoApiAction extends SearchEverywhereBaseAction implements DumbAware {

    public GotoApiAction() {
        Presentation presentation = getTemplatePresentation();
        presentation.setText("Go to API...");
        presentation.setDescription("Quickly navigate to API documentation");
        addTextOverride(ActionPlaces.MAIN_MENU, "API...");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        showInSearchEverywherePopup(ApiSearchEverywhereContributor.ID, e, true, true);
    }
}