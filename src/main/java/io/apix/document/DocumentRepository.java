package io.apix.document;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Document repository.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentRepository {

    private final Project project;

    public static DocumentRepository getInstance(Project project) {
        return new DocumentRepository(project);
    }

    /**
     * Fetch all document.
     */
    public List<Document> get() {
        DocumentSettings settings = DocumentSettings.getInstance(project);
        return settings.getDocuments();
    }

    /**
     * Fetch one document.
     */
    @Nullable
    public Document get(@NonNull String id) {
        DocumentSettings settings = DocumentSettings.getInstance(project);
        return settings.getDocuments().stream()
                .filter(datasource -> id.equals(datasource.getId()))
                .findFirst().orElse(null);
    }

    /**
     * Fetch one document with credentials loaded from PasswordSafe.
     */
    @Nullable
    public Document getWithCredentials(@NonNull String id) {
        Document document = get(id);
        loadCredentials(document);
        return document;
    }

    /**
     * Save document.
     */
    public void save(@NonNull Document document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID().toString());
        }
        migrateLegacyConnection(document);
        if (usesConnection(document)) {
            DocumentCredentialService.delete(document.getId());
            clearCredentials(document);
        } else {
            saveCredentials(document);
        }
        clearLegacyCredentials(document);
        DocumentSettings settings = DocumentSettings.getInstance(project);
        List<Document> documents = settings.getDocuments();

        boolean exists = false;
        for (int i = 0; i < documents.size(); i++) {
            if (document.getId().equals(documents.get(i).getId())) {
                documents.set(i, document);
                exists = true;
                break;
            }
        }
        if (!exists) {
            documents.add(document);
        }
        settings.loadState(settings);
    }

    /**
     * Delete document.
     */
    public void delete(@NonNull String id) {
        DocumentSettings settings = DocumentSettings.getInstance(project);
        settings.getDocuments().removeIf(datasource -> id.equals(datasource.getId()));
        DocumentCredentialService.delete(id);
        settings.loadState(settings);
    }

    /**
     * Delete document.
     */
    public void deletes(@NonNull List<String> ids) {
        DocumentSettings settings = DocumentSettings.getInstance(project);
        for (String id : ids) {
            settings.getDocuments().removeIf(document -> id.equals(document.getId()));
            DocumentCredentialService.delete(id);
        }
        settings.loadState(settings);
    }

    /**
     * Reorder documents: move sourceId before targetId.
     * targetId == null → move to end
     */
    public void reorder(String sourceId, String targetId) {
        DocumentSettings settings = DocumentSettings.getInstance(project);
        List<Document> documents = settings.getDocuments();

        if (documents == null || documents.size() <= 1) {
            return;
        }

        int fromIndex = -1;
        int targetIndex = -1;

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            if (doc.getId().equals(sourceId)) {
                fromIndex = i;
            }
            if (doc.getId().equals(targetId)) {
                targetIndex = i;
            }
        }

        if (fromIndex < 0) {
            return;
        }

        if (targetId == null) {
            targetIndex = documents.size();
        }

        if (targetId != null && targetIndex < 0) {
            return;
        }

        if (fromIndex == targetIndex || fromIndex + 1 == targetIndex) {
            return;
        }

        Document doc = documents.remove(fromIndex);

        int insertIndex = targetIndex > fromIndex ? targetIndex - 1 : targetIndex;

        // 边界保护（保险）
        if (insertIndex > documents.size()) {
            insertIndex = documents.size();
        }

        documents.add(insertIndex, doc);
    }

    private void migrateLegacyConnection(Document document) {
        DocumentSourceType type = document.getType();
        if (type == DocumentSourceType.Apifox) {
            migrateApifoxConnection(document);
        } else if (type == DocumentSourceType.SwaggerHub) {
            migrateSwaggerHubConnection(document);
        }
    }

    private void migrateApifoxConnection(Document document) {
        Document.ApifoxConfig config = document.getApifoxConfig();
        if (config == null || StringUtils.isNotBlank(config.getConnectionId())) {
            return;
        }
        if (StringUtils.isAnyBlank(config.getServiceUrl(), config.getAccessToken())) {
            return;
        }
        Connection connection = ConnectionRepository.getInstance().getOrCreate(
                DocumentSourceType.Apifox,
                config.getServiceUrl(),
                config.getAccessToken(),
                "Apifox");
        config.setConnectionId(connection.getId());
    }

    private void migrateSwaggerHubConnection(Document document) {
        Document.SwaggerHubConfig config = document.getSwaggerHubConfig();
        if (config == null || StringUtils.isNotBlank(config.getConnectionId())) {
            return;
        }
        if (StringUtils.isAnyBlank(config.getServiceUrl(), config.getApiKey())) {
            return;
        }
        Connection connection = ConnectionRepository.getInstance().getOrCreate(
                DocumentSourceType.SwaggerHub,
                config.getServiceUrl(),
                config.getApiKey(),
                "SwaggerHub");
        config.setConnectionId(connection.getId());
    }

    public void loadCredentials(Document document) {
        if (document == null) {
            return;
        }
        Document.ApifoxConfig apifoxConfig = document.getApifoxConfig();
        if (apifoxConfig != null && StringUtils.isEmpty(apifoxConfig.getAccessToken())) {
            String accessToken = DocumentCredentialService.get(document, DocumentCredentialService.APIFOX_ACCESS_TOKEN);
            if (StringUtils.isNotEmpty(accessToken)) {
                apifoxConfig.setAccessToken(accessToken);
            } else if (StringUtils.isNotEmpty(apifoxConfig.getLegacyAccessToken())) {
                apifoxConfig.setAccessToken(apifoxConfig.getLegacyAccessToken());
            }
        }

        Document.SwaggerHubConfig swaggerHubConfig = document.getSwaggerHubConfig();
        if (swaggerHubConfig != null && StringUtils.isEmpty(swaggerHubConfig.getApiKey())) {
            String apiKey = DocumentCredentialService.get(document, DocumentCredentialService.SWAGGERHUB_API_KEY);
            if (StringUtils.isNotEmpty(apiKey)) {
                swaggerHubConfig.setApiKey(apiKey);
            } else if (StringUtils.isNotEmpty(swaggerHubConfig.getLegacyApiKey())) {
                swaggerHubConfig.setApiKey(swaggerHubConfig.getLegacyApiKey());
            }
        }
    }

    private boolean usesConnection(Document document) {
        if (document.getType() == DocumentSourceType.Apifox) {
            Document.ApifoxConfig apifoxConfig = document.getApifoxConfig();
            return apifoxConfig != null && StringUtils.isNotBlank(apifoxConfig.getConnectionId());
        }

        if (document.getType() == DocumentSourceType.SwaggerHub) {
            Document.SwaggerHubConfig swaggerHubConfig = document.getSwaggerHubConfig();
            return swaggerHubConfig != null && StringUtils.isNotBlank(swaggerHubConfig.getConnectionId());
        }

        return false;
    }

    private void clearCredentials(Document document) {
        if (document.getType() == DocumentSourceType.Apifox) {
            Document.ApifoxConfig apifoxConfig = document.getApifoxConfig();
            if (apifoxConfig != null) {
                apifoxConfig.setServiceUrl(null);
                apifoxConfig.setAccessToken(null);
            }
        }

        if (document.getType() == DocumentSourceType.SwaggerHub) {
            Document.SwaggerHubConfig swaggerHubConfig = document.getSwaggerHubConfig();
            if (swaggerHubConfig != null) {
                swaggerHubConfig.setServiceUrl(null);
                swaggerHubConfig.setApiKey(null);
            }
        }
    }

    private void saveCredentials(Document document) {
        Document.ApifoxConfig apifoxConfig = document.getApifoxConfig();
        if (apifoxConfig != null) {
            DocumentCredentialService.set(document, DocumentCredentialService.APIFOX_ACCESS_TOKEN, apifoxConfig.getAccessToken());
        }

        Document.SwaggerHubConfig swaggerHubConfig = document.getSwaggerHubConfig();
        if (swaggerHubConfig != null) {
            DocumentCredentialService.set(document, DocumentCredentialService.SWAGGERHUB_API_KEY, swaggerHubConfig.getApiKey());
        }
    }

    private void clearLegacyCredentials(Document document) {
        Document.ApifoxConfig apifoxConfig = document.getApifoxConfig();
        if (apifoxConfig != null) {
            apifoxConfig.setLegacyAccessToken(null);
        }

        Document.SwaggerHubConfig swaggerHubConfig = document.getSwaggerHubConfig();
        if (swaggerHubConfig != null) {
            swaggerHubConfig.setLegacyApiKey(null);
        }
    }

}
