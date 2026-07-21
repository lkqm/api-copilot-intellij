package io.apix.document;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for global source connections.
 */
@Service(Service.Level.APP)
public final class ConnectionRepository {

    private static final String SERVICE_PREFIX = "ApixConnection";
    private static final String CREDENTIAL_KEY = "credential";

    public static ConnectionRepository getInstance() {
        return ApplicationManager.getApplication().getService(ConnectionRepository.class);
    }

    public List<Connection> list(DocumentSourceType type) {
        return settings().connections.stream()
                .filter(c -> c != null && c.getType() == type)
                .map(this::cloneWithoutCredential)
                .sorted(Comparator.comparing(
                        c -> c.getCreatedAt() != null ? c.getCreatedAt() : 0L))
                .collect(Collectors.toList());
    }

    public List<Connection> listSupported() {
        List<Connection> result = new ArrayList<>();
        result.addAll(list(DocumentSourceType.Apifox));
        result.addAll(list(DocumentSourceType.SwaggerHub));
        result.addAll(list(DocumentSourceType.OpenAPI));
        return result;
    }

    public @Nullable Connection get(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        for (Connection connection : settings().connections) {
            if (connection != null && id.equals(connection.getId())) {
                return cloneWithoutCredential(connection);
            }
        }
        return null;
    }

    public @Nullable Connection getWithCredential(String id) {
        Connection connection = get(id);
        return connection != null ? cloneWithCredential(connection) : null;
    }

    public Connection getOrCreate(DocumentSourceType type, String baseUrl, String credential, String defaultName) {
        Connection existing = findByCredential(type, baseUrl, credential);
        if (existing != null) {
            return existing;
        }

        Connection connection = new Connection();
        connection.setType(type);
        connection.setName(nextName(type, defaultName));
        connection.setBaseUrl(StringUtils.trim(baseUrl));
        connection.setAuthType(defaultAuthType(type));
        connection.setCredential(credential);
        save(connection);
        return connection;
    }

    public void save(Connection connection) {
        if (connection == null) {
            return;
        }
        if (StringUtils.isBlank(connection.getId())) {
            connection.setId(UUID.randomUUID().toString());
        }
        if (connection.getCreatedAt() == null) {
            connection.setCreatedAt(System.currentTimeMillis());
        }

        if (connection.getCredential() != null) {
            saveCredential(connection.getId(), connection.getCredential());
        }

        Connection stored = cloneWithoutCredential(connection);
        if (stored.getAuthType() == null) {
            stored.setAuthType(defaultAuthType(stored.getType()));
        }
        List<Connection> connections = settings().connections;
        for (int i = 0; i < connections.size(); i++) {
            Connection current = connections.get(i);
            if (current != null && connection.getId().equals(current.getId())) {
                connections.set(i, stored);
                return;
            }
        }
        connections.add(stored);
    }

    public void delete(String id) {
        if (StringUtils.isBlank(id)) {
            return;
        }
        settings().connections.removeIf(c -> c != null && id.equals(c.getId()));
        PasswordSafe.getInstance().set(attributes(id), null);
    }

    private @Nullable Connection findByCredential(DocumentSourceType type, String baseUrl, String credential) {
        if (type == null || StringUtils.isBlank(baseUrl) || StringUtils.isBlank(credential)) {
            return null;
        }
        String normalizedBaseUrl = StringUtils.trim(baseUrl);
        for (Connection connection : settings().connections) {
            if (connection == null || connection.getType() != type) {
                continue;
            }
            if (!StringUtils.equals(StringUtils.trim(connection.getBaseUrl()), normalizedBaseUrl)) {
                continue;
            }
            Connection copy = cloneWithCredential(connection);
            if (StringUtils.equals(copy.getCredential(), credential)) {
                return copy;
            }
        }
        return null;
    }

    private String nextName(DocumentSourceType type, String defaultName) {
        String baseName = StringUtils.defaultIfBlank(defaultName, "Connection");
        List<Connection> connections = list(type);
        boolean exists = connections.stream().anyMatch(c -> StringUtils.equals(c.getName(), baseName));
        if (!exists) {
            return baseName;
        }
        int index = 2;
        while (true) {
            String name = baseName + " " + index;
            boolean nameExists = connections.stream().anyMatch(c -> StringUtils.equals(c.getName(), name));
            if (!nameExists) {
                return name;
            }
            index++;
        }
    }

    private ConnectionSettings settings() {
        return ApplicationManager.getApplication().getService(ConnectionSettings.class);
    }

    private Connection cloneWithCredential(Connection source) {
        Connection copy = cloneWithoutCredential(source);
        copy.setCredential(loadCredential(copy.getId()));
        return copy;
    }

    private Connection cloneWithoutCredential(Connection source) {
        Connection copy = new Connection();
        copy.setId(source.getId());
        copy.setType(source.getType());
        copy.setName(source.getName());
        copy.setBaseUrl(source.getBaseUrl());
        copy.setAuthType(source.getAuthType() != null ? source.getAuthType() : defaultAuthType(source.getType()));
        copy.setUsername(source.getUsername());
        copy.setHeaderName(source.getHeaderName());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }

    private ConnectionAuthType defaultAuthType(DocumentSourceType type) {
        if (type == DocumentSourceType.Apifox) {
            return ConnectionAuthType.Bearer;
        }
        if (type == DocumentSourceType.SwaggerHub) {
            return ConnectionAuthType.Header;
        }
        return ConnectionAuthType.Basic;
    }

    private String loadCredential(String connectionId) {
        if (StringUtils.isBlank(connectionId)) {
            return "";
        }
        String credential = PasswordSafe.getInstance().getPassword(attributes(connectionId));
        return credential != null ? credential : "";
    }

    private void saveCredential(String connectionId, String credential) {
        if (StringUtils.isBlank(connectionId)) {
            return;
        }
        if (StringUtils.isBlank(credential)) {
            PasswordSafe.getInstance().set(attributes(connectionId), null);
            return;
        }
        PasswordSafe.getInstance().set(attributes(connectionId), new Credentials("", credential));
    }

    private static CredentialAttributes attributes(String connectionId) {
        return new CredentialAttributes(SERVICE_PREFIX + "." + connectionId + "." + CREDENTIAL_KEY);
    }
}
