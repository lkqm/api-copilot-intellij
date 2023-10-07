package apicopilot.search;

import apicopilot.icon.HttpMethodIcons;
import apicopilot.window.ApiToolWindow;
import apicopilot.window.ApiToolWindowManager;
import com.intellij.ide.actions.SearchEverywherePsiRenderer;
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.Processor;
import com.intellij.util.ui.UIUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ApiSearchEverywhereContributor implements WeightedSearchEverywhereContributor<ApiNavigationData> {

    public static final String ID = "apicopilot.ApiSearchEverywhereContributor";
    public static final String GROUP_NAME = "API";

    private final AnActionEvent actionEvent;
    private final Project myProject;
    private final ApiNavigationResolver apiResolver;

    private List<ApiNavigationData> apis;


    protected ApiSearchEverywhereContributor(@NotNull AnActionEvent actionEvent, @NotNull ApiNavigationResolver apiResolver) {
        this.actionEvent = actionEvent;
        this.myProject = actionEvent.getRequiredData(CommonDataKeys.PROJECT);
        this.apiResolver = apiResolver;
    }


    @Override
    public @NotNull String getSearchProviderId() {
        return ID;
    }

    @Override
    public @NotNull @Nls String getGroupName() {
        return GROUP_NAME;
    }

    @Override
    public int getSortWeight() {
        return 1000;
    }

    @Override
    public boolean showInFindResults() {
        return false;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true;
    }

    @Override
    public boolean isEmptyPatternSupported() {
        return true;
    }

    @Override
    public boolean processSelectedItem(@NotNull ApiNavigationData selected, int modifiers, @NotNull String searchText) {
        ApiToolWindow toolWindow = ApiToolWindowManager.getInstance().getToolWindow(myProject);
        if (toolWindow == null) {
            return false;
        }
        return ApiToolWindowManager.getInstance().showWindow(myProject,
                () -> toolWindow.navigateToApi(selected.getConnectionId(), selected.getMethod(), selected.getPath()));
    }

    @Override
    public @NotNull ListCellRenderer<Object> getElementsRenderer() {
        return new SearchEverywherePsiRenderer(this) {

            @Override
            protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer, JList list, Object value, int index, boolean selected, boolean hasFocus) {
                Color fgColor = list.getForeground();
                Color bgColor = UIUtil.getListBackground();
                SimpleTextAttributes nameAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, fgColor);

                ItemMatchers itemMatchers = getItemMatchers(list, value);
                ApiNavigationData apiNavigationItem = (ApiNavigationData) value;
                String name = apiNavigationItem.getPath();
                String locationString = " " + apiNavigationItem.getSummary();

                SpeedSearchUtil.appendColoredFragmentForMatcher(name, renderer, nameAttributes, itemMatchers.nameMatcher, bgColor, selected);
                renderer.setIcon(HttpMethodIcons.getHttpMethodIcon(apiNavigationItem.getMethod()));

                if (StringUtils.isNotEmpty(locationString)) {
                    FontMetrics fm = list.getFontMetrics(list.getFont());
                    int maxWidth = list.getWidth() - fm.stringWidth(name) - myRightComponentWidth - 36;
                    int fullWidth = fm.stringWidth(locationString);
                    if (fullWidth < maxWidth) {
                        SpeedSearchUtil.appendColoredFragmentForMatcher(locationString, renderer, SimpleTextAttributes.GRAYED_ATTRIBUTES, itemMatchers.nameMatcher, bgColor, selected);
                    } else {
                        int adjustedWidth = Math.max(locationString.length() * maxWidth / fullWidth - 1, 3);
                        locationString = StringUtil.trimMiddle(locationString, adjustedWidth);
                        SpeedSearchUtil.appendColoredFragmentForMatcher(locationString, renderer, SimpleTextAttributes.GRAYED_ATTRIBUTES, itemMatchers.nameMatcher, bgColor, selected);
                    }
                }
                return true;
            }
        };
    }

    @Override
    public @Nullable Object getDataForItem(@NotNull ApiNavigationData element, @NotNull String dataId) {
        return null;
    }

    @SneakyThrows
    @Override
    public void fetchWeightedElements(@NotNull String pattern, @NotNull ProgressIndicator progressIndicator, @NotNull Processor<? super FoundItemDescriptor<ApiNavigationData>> consumer) {
        if (!shouldProvideElements(pattern)) {
            return;
        }
        if (apis == null) {
            apis = ApplicationManager.getApplication().runReadAction((ThrowableComputable<List<ApiNavigationData>, Throwable>) apiResolver::getApis);
        }
        MinusculeMatcher matcher = NameUtil.buildMatcher("*" + pattern + "*", NameUtil.MatchingCaseSensitivity.NONE);
        for (ApiNavigationData restItem : this.apis) {
            if (matcher.matches(restItem.getPath()) || matcher.matches(restItem.getSummary())) {
                if (!consumer.process(new FoundItemDescriptor<>(restItem, 0))) {
                    return;
                }
            }
        }
    }

    private boolean shouldProvideElements(String pattern) {
        if (StringUtils.isNotBlank(pattern)) {
            return true;
        }
        SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(myProject);
        if (seManager.isShown()) {
            return getSearchProviderId().equals(seManager.getSelectedTabID());
        } else {
            return !ActionsBundle.message("action.SearchEverywhere.text").equals(actionEvent.getPresentation().getText());
        }
    }

}
