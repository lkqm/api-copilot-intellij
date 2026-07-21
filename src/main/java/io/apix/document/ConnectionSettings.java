package io.apix.document;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Application-level persistent state for source connections.
 * Credentials are intentionally excluded from this XML state.
 */
@Service(Service.Level.APP)
@State(name = "ApixConnectionSettings", storages = @Storage("apiConnections.xml"))
public final class ConnectionSettings
        implements PersistentStateComponent<ConnectionSettings> {

    public List<Connection> connections = new ArrayList<>();

    @Override
    public @NotNull ConnectionSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ConnectionSettings state) {
        XmlSerializerUtil.copyBean(state, this);
        if (connections == null) {
            connections = new ArrayList<>();
        }
    }
}
