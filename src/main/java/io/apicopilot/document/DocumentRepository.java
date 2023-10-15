package io.apicopilot.document;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

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
     * Save document.
     */
    public void save(@NonNull Document document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID().toString());
        }
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
        settings.loadState(settings);
    }

    /**
     * Delete document.
     */
    public void deletes(@NonNull List<String> ids) {
        DocumentSettings settings = DocumentSettings.getInstance(project);
        for (String id : ids) {
            settings.getDocuments().removeIf(document -> id.equals(document.getId()));
        }
        settings.loadState(settings);
    }

}
