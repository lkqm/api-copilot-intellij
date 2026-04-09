package io.apicopilot.startup;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import io.apicopilot.document.*;
import io.apicopilot.document.topic.DocumentTopic;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Document check update scheduler.
 */
@Slf4j
public class DocumentUpdateScheduler implements Disposable {

    private static final long INITIAL_DELAY_SECONDS = 15;
    private static final long INTERVAL_SECONDS = 600;

    private final Project project;
    private final ScheduledFuture<?> scheduledFuture;

    public DocumentUpdateScheduler(Project project) {
        this.project = project;
        scheduledFuture = AppExecutorUtil.getAppScheduledExecutorService()
                .scheduleWithFixedDelay(this::checkAll, INITIAL_DELAY_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void checkAll() {
        if (project.isDisposed()) {
            return;
        }
        DocumentManager manager = DocumentManager.getInstance(project);
        List<Document> documents = DocumentRepository.getInstance(project).get();
        if (documents == null || documents.isEmpty()) {
            return;
        }

        for (Document document : documents) {
            // skip syncing
            if(document.getSyncStatus() == SyncStatus.SYNCING) {
                continue;
            }

            // skip has update for check
            if (document.isHasUpdate() && !document.isAutoSyncEnabled()) {
                continue;
            }

            // do check and update
            if(!document.isAutoSyncEnabled()) {
                manager.checkUpdate(document);
            } else {
                manager.reloadDocument(document);
            }
        }
    }

    @Override
    public void dispose() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }
}
