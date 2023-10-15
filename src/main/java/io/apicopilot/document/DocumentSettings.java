package io.apicopilot.document;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Document persistent state.
 */
@Getter
@Setter
@Service(Service.Level.PROJECT)
@State(name = "ApiCopilotDocuments", storages = @Storage("apiDocuments.xml"))
public final class DocumentSettings implements PersistentStateComponent<DocumentSettings> {

    /**
     * 文档信息
     */
    private List<Document> documents;

    public static DocumentSettings getInstance(@NonNull Project project) {
        DocumentSettings documentSettings = ServiceManager.getService(project, DocumentSettings.class);
        if (documentSettings == null) {
            documentSettings = new DocumentSettings();
        }
        if (documentSettings.getDocuments() == null) {
            documentSettings.setDocuments(new ArrayList<>());
        }
        return documentSettings;
    }


    @Override
    @NotNull
    public DocumentSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DocumentSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}