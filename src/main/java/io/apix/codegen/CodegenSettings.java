package io.apix.codegen;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
@State(name = "ApiCopilotCodegenSettings", storages = @Storage("apiCodegenSettings.xml"))
public final class CodegenSettings implements PersistentStateComponent<CodegenSettings> {

    public String requestLastLanguage = "";
    public String modelLastLanguage = "";

    public static CodegenSettings getInstance(@NonNull Project project) {
        return project.getService(CodegenSettings.class);
    }

    @Override
    public @NotNull CodegenSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CodegenSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
