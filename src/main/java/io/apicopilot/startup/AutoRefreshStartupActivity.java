package io.apicopilot.startup;

import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentManager;
import io.apicopilot.document.LoadResult;
import io.apicopilot.util.NotificationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Auto refresh documents with autoRefresh=true when project opens.
 */
public class AutoRefreshStartupActivity implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            DocumentManager manager = DocumentManager.getInstance(project);
            List<Document> documents = manager.getDocuments();
            if (documents.isEmpty()) {
                return;
            }
            Map<Document, LoadResult> results = Maps.newLinkedHashMap();
            for (Document document : documents) {
                if (document.isAutoRefresh()) {
                    LoadResult result = manager.reloadDocument(document);
                    if(!result.isSuccess()) {
                        results.put(document, result);
                    }
                }
            }

            if(!results.isEmpty()) {
                String tips = results.entrySet().stream()
                        .map(entry -> entry.getKey().getName() + ": " + entry.getValue().getFailReason())
                        .collect(Collectors.joining("\n\n"));
                NotificationUtils.notifyError("Refresh document failed!", tips);
            }
        });
    }
}
