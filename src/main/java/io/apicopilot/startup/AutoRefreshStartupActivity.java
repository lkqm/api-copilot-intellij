package io.apicopilot.startup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Auto refresh documents with autoRefresh=true when project opens.
 */
public class AutoRefreshStartupActivity implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            DocumentManager manager = DocumentManager.getInstance(project);
            List<Document> documents = manager.getDocuments();
            for (Document document : documents) {
                if (document.isAutoRefresh()) {
                    manager.reloadDocument(document);
                }
            }
        });
    }
}
