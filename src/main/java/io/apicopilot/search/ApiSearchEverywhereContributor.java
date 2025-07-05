package io.apicopilot.search;

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
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.Processor;
import com.intellij.util.ui.UIUtil;
import io.apicopilot.icon.HttpMethodIcons;
import io.apicopilot.window.ApiView;
import lombok.SneakyThrows;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ApiSearchEverywhereContributor implements WeightedSearchEverywhereContributor<ApiNavigationData> {

    public static final String ID = "apicopilot.ApiSearchEverywhereContributor";
    public static final String GROUP_NAME = "APIs";

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
        ApiView.getInstance(myProject).select(selected.getDocumentId(), selected.getPath(), selected.getMethod());
        return true;
    }

    @Override
    public @NotNull ListCellRenderer<Object> getElementsRenderer() {
        return new SearchEverywherePsiRenderer(this) {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ApiNavigationData api = (ApiNavigationData) value;
                FontMetrics fm = list.getFontMetrics(list.getFont());
                Color fgColor = list.getForeground();
                Color bgColor = UIUtil.getListBackground();

                // 右侧显示：documentName
                int rightWidth = 0;
                final int rightMaxWidth = 80;
                String documentName = api.getDocumentName();
                if (StringUtils.isNotEmpty(api.getDocumentName())) {
                    rightWidth = fm.stringWidth(documentName);
                    if (rightWidth > rightMaxWidth) {
                        int adjustedWidth = Math.max(documentName.length() * rightMaxWidth / rightWidth, 3);
                        documentName = documentName.substring(0, Math.min(adjustedWidth, documentName.length()));
                        rightWidth = rightMaxWidth;
                    }
                }
                JLabel right = new JLabel(documentName);
                right.setForeground(SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());

                // 左侧显示: method + path + summary
                ItemMatchers itemMatchers = getItemMatchers(list, value);
                SimpleColoredComponent left = new SimpleColoredComponent();
                left.setIcon(HttpMethodIcons.getHttpMethodIcon(api.getMethod()));

                SimpleTextAttributes pathTextAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
                if (BooleanUtils.isTrue(api.getDeprecated())) {
                    pathTextAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());
                }
                SpeedSearchUtil.appendColoredFragmentForMatcher(api.getPath(), left, pathTextAttributes, itemMatchers.nameMatcher, bgColor, isSelected);

                if (StringUtils.isNotEmpty(api.getSummary())) {
                    String summary = " " + api.getSummary();
                    int width = fm.stringWidth(summary);
                    int remainWidth = list.getWidth() - fm.stringWidth(api.getPath()) - myRightComponentWidth - rightWidth - 45;
                    if (width > remainWidth) {
                        int adjustedWidth = Math.max(summary.length() * remainWidth / width - 1, 3);
                        summary = StringUtil.shortenTextWithEllipsis(summary, adjustedWidth, 0, true);
                    }
                    SpeedSearchUtil.appendColoredFragmentForMatcher(summary, left, SimpleTextAttributes.GRAYED_ATTRIBUTES, itemMatchers.nameMatcher, bgColor, isSelected);
                }


                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(true);
                panel.add(left, BorderLayout.CENTER);
                panel.add(right, BorderLayout.EAST);

                // 选中控制
                if (isSelected) {
                    panel.setBackground(list.getSelectionBackground());
                    left.setBackground(list.getSelectionBackground());
                    right.setBackground(list.getSelectionBackground());
                } else {
                    panel.setBackground(list.getBackground());
                    left.setBackground(list.getBackground());
                    right.setBackground(list.getBackground());
                }
                return panel;
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
