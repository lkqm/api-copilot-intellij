package io.apicopilot.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

/**
 * 项目启动时初始化文档更新检测调度器.
 */
public class DocumentUpdateStartupActivity implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        DocumentUpdateScheduler scheduler = new DocumentUpdateScheduler(project);
        Disposer.register(project, scheduler);
    }
}
