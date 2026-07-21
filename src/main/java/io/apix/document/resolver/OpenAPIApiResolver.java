package io.apix.document.resolver;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.apix.document.Connection;
import io.apix.document.ConnectionAuthType;
import io.apix.document.ConnectionRepository;
import io.apix.document.Document;
import io.apix.util.HttpUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolve API document from OpenAPI.
 */
@AllArgsConstructor
public class OpenAPIApiResolver extends AbstractApiResolver {

    private final Project project;
    private final Document document;

    @SneakyThrows
    @Override
    protected ResolveResult getContent(boolean reload) {
        if (StringUtils.isNotEmpty(document.getContent()) && !reload) {
            return ResolveResult.ok(document.getContent());
        }

        Document.OpenApiConfig openApiConfig = document.getOpenApiConfig();
        String path = openApiConfig.getPath();
        if (StringUtils.isEmpty(path)) {
            return ResolveResult.fail("invalid document config");
        }

        if (openApiConfig.isRemote()) {
            try {
                Connection connection = ConnectionRepository.getInstance().getWithCredential(openApiConfig.getConnectionId());
                if (StringUtils.isNotBlank(openApiConfig.getConnectionId()) && connection == null) {
                    return ResolveResult.fail("auth is unavailable");
                }
                Map<String, String> headers = buildHeaders(connection);
                byte[] data = HttpUtils.get(path, headers, Duration.ofSeconds(10));
                String content = new String(data, StandardCharsets.UTF_8);
                return ResolveResult.ok(content);
            } catch (IOException e) {
                return ResolveResult.fail("download file failed: " + ExceptionUtils.getMessage(e));
            }
        } else {
            VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
            if (projectDir != null) {
                VirtualFile file = projectDir.findFileByRelativePath(path);
                if (file != null) {
                    String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                    return ResolveResult.ok(content);
                }
            }
            return ResolveResult.fail("file not exists: " + path);
        }
    }

    private Map<String, String> buildHeaders(Connection connection) {
        if (connection == null) {
            return null;
        }
        ConnectionAuthType authType = connection.getAuthType();
        if (authType == null || authType == ConnectionAuthType.None) {
            return null;
        }

        Map<String, String> headers = new HashMap<>();
        String credential = connection.getCredential();
        if (authType == ConnectionAuthType.Bearer && StringUtils.isNotBlank(credential)) {
            headers.put("Authorization", "Bearer " + credential);
        } else if (authType == ConnectionAuthType.Basic
                && StringUtils.isNotBlank(connection.getUsername())
                && StringUtils.isNotBlank(credential)) {
            String value = connection.getUsername() + ":" + credential;
            headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
        } else if (authType == ConnectionAuthType.Header
                && StringUtils.isNotBlank(connection.getHeaderName())
                && StringUtils.isNotBlank(credential)) {
            headers.put(connection.getHeaderName(), credential);
        }
        return headers.isEmpty() ? null : headers;
    }

}
