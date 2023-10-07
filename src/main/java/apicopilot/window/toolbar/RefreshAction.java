/*
  Copyright (C), 2018-2020, ZhangYuanSheng
  FileName: RefreshAction
  Author:   ZhangYuanSheng
  Date:     2020/8/18 15:34
  Description: 
  History:
  <author>          <time>          <version>          <desc>
  作者姓名            修改时间           版本号              描述
 */
package apicopilot.window.toolbar;


import apicopilot.window.ApiToolWindow;
import apicopilot.window.ApiToolWindowManager;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 刷新按钮
 */
public class RefreshAction extends DumbAwareAction {

    public RefreshAction() {
        getTemplatePresentation().setText("Refresh");
        getTemplatePresentation().setIcon(AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        ApiToolWindow toolWindow = ApiToolWindowManager.getInstance().getToolWindow(project);
        if (toolWindow == null) {
            return;
        }
        toolWindow.refreshTree();
    }
}
