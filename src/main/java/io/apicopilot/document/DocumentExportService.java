package io.apicopilot.document;

import com.intellij.openapi.project.Project;
import io.apicopilot.util.FileWriteUtils;
import io.apicopilot.util.JsonUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Export synced documents to the project .apis/ directory for AI tools.
 */
@Slf4j
@RequiredArgsConstructor
public class DocumentExportService {

    private static final String EXPORT_DIR = "api-specs";
    private static final String MANIFEST_FILE = EXPORT_DIR + "/manifest.json";

    private final Project project;

    public static DocumentExportService getInstance(Project project) {
        return new DocumentExportService(project);
    }

    public void export(Document document) {
        if (document == null || !document.isExportEnabled()) {
            return;
        }

        try {
            OpenAPI openApi = document.getOpenApi();
            if (openApi == null) {
                throw new IllegalStateException("OpenAPI content is not available");
            }

            String fileName = buildFileName(document.getName());
            String content = Json.pretty(openApi);
            FileWriteUtils.write(project, EXPORT_DIR + "/" + fileName, content);
            document.setExportLastSuccessTime(System.currentTimeMillis());
            document.setExportLastFailMessage(null);
            writeManifest();
        } catch (Exception e) {
            document.setExportLastFailMessage(e.getMessage());
            log.warn("Failed to export document {} to {}", document.getName(), EXPORT_DIR, e);
        }

        DocumentRepository.getInstance(project).save(document);
    }

    private String buildFileName(String name) {
        String normalized = StringUtils.trimToEmpty(name)
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            normalized = "openapi";
        }
        return normalized + ".json";
    }

    private void writeManifest() {
        List<Document> documents = DocumentRepository.getInstance(project).get();
        List<ManifestItem> items = new ArrayList<>();
        if (documents != null) {
            for (Document document : documents) {
                if (!document.isExportEnabled() || StringUtils.isBlank(document.getName())) {
                    continue;
                }
                ManifestItem item = new ManifestItem();
                item.setName(document.getName());
                item.setType(document.getType() != null ? document.getType().name() : null);
                item.setFile(buildFileName(document.getName()));
                item.setLastSuccessTime(document.getLastSuccessTime());
                item.setExportLastSuccessTime(document.getExportLastSuccessTime());
                item.setSyncStatus(document.getSyncStatus() != null ? document.getSyncStatus().name() : null);
                items.add(item);
            }
        }
        FileWriteUtils.write(project, MANIFEST_FILE, JsonUtils.toJson(items));
    }

    @lombok.Data
    private static class ManifestItem {
        private String name;
        private String type;
        private String file;
        private Long lastSuccessTime;
        private Long exportLastSuccessTime;
        private String syncStatus;
    }
}
