package apicopilot.search;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ApiSearchEverywhereContributorFactory implements SearchEverywhereContributorFactory<ApiNavigationData> {
    @NotNull
    @Override
    public SearchEverywhereContributor<ApiNavigationData> createContributor(@NotNull AnActionEvent actionEvent) {
        Project project = actionEvent.getRequiredData(CommonDataKeys.PROJECT);
        ApiNavigationResolver apiResolver = new ApiNavigationResolverImpl(project);
        return new ApiSearchEverywhereContributor(actionEvent, apiResolver);
    }
}
