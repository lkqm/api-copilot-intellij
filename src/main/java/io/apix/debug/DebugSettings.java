package io.apix.debug;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Persistent debug settings: auth configs and environment configs, both per document.
 */
@Service(Service.Level.PROJECT)
@State(name = "ApiCopilotDebugSettings", storages = @Storage("apiDebugSettings.xml"))
public final class DebugSettings implements PersistentStateComponent<DebugSettings> {

    /** documentId → AuthConfig */
    public Map<String, AuthConfig> authConfigs = new HashMap<>();

    /** documentId → EnvConfig */
    public Map<String, EnvConfig> envConfigs = new HashMap<>();


    public static DebugSettings getInstance(@NonNull Project project) {
        return project.getService(DebugSettings.class);
    }

    // ── Auth ───────────────────────────────────────────────────────────────

    public AuthConfig getAuthConfig(String documentId) {
        if (documentId == null) return new AuthConfig();
        AuthConfig config = authConfigs.get(documentId);
        return config != null ? config : new AuthConfig();
    }

    public void setAuthConfig(Project project, String documentId, AuthConfig config) {
        if (documentId == null || config == null) return;
        authConfigs.put(documentId, config);
        project.getMessageBus()
               .syncPublisher(DocAuthChangeListener.TOPIC)
               .docAuthChanged(documentId, config);
    }

    // ── Env ────────────────────────────────────────────────────────────────

    public EnvConfig getEnvConfig(String documentId) {
        if (documentId == null) return new EnvConfig();
        return envConfigs.computeIfAbsent(documentId, id -> new EnvConfig());
    }

    public List<Environment> getEnvironments(String documentId) {
        return getEnvConfig(documentId).environments;
    }

    public void setEnvironments(String documentId, List<Environment> environments) {
        getEnvConfig(documentId).environments = environments;
    }

    public String getActiveEnvId(String documentId) {
        return getEnvConfig(documentId).activeEnvId;
    }

    public void setActiveEnvId(String documentId, String activeEnvId) {
        getEnvConfig(documentId).activeEnvId = activeEnvId != null ? activeEnvId : "";
    }

    public Environment findEnvById(String documentId, String envId) {
        if (envId == null || envId.isEmpty()) return null;
        for (Environment e : getEnvironments(documentId)) {
            if (envId.equals(e.getId())) return e;
        }
        return null;
    }

    public Environment getActiveEnvironment(String documentId) {
        return findEnvById(documentId, getActiveEnvId(documentId));
    }

    /** Returns enabled variables of the active environment as a name → value map. */
    public Map<String, String> getActiveVariables(String documentId) {
        Environment env = getActiveEnvironment(documentId);
        if (env == null) return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<>();
        for (EnvVariable v : env.getVariables()) {
            if (v.isEnabled() && v.getName() != null && !v.getName().isEmpty()) {
                result.put(v.getName(), v.getValue() != null ? v.getValue() : "");
            }
        }
        return result;
    }

    /**
     * Replaces all {@code {{variableName}}} placeholders in {@code text} with values
     * from the active environment of the given document. Undefined variables are left as-is.
     */
    public String resolve(String documentId, String text) {
        if (text == null || !text.contains("{{")) return text;
        Map<String, String> vars = getActiveVariables(documentId);
        if (vars.isEmpty()) return text;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            text = text.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return text;
    }

    // ── PersistentStateComponent ───────────────────────────────────────────

    @Override
    public @NotNull DebugSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DebugSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
