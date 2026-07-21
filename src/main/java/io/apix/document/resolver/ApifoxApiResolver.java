package io.apix.document.resolver;

import io.apix.document.Document;
import io.apix.document.Connection;
import io.apix.document.ConnectionRepository;
import io.apix.util.HttpUtils;
import io.apix.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolve API document from OpenAPI.
 */
@AllArgsConstructor
public class ApifoxApiResolver extends AbstractApiResolver {

    private final Document document;

    public static final String EXPORT_PATH = "/v1/projects/%s/export-openapi";

    @SneakyThrows
    @Override
    protected ResolveResult getContent(boolean reload) {
        if (StringUtils.isNotEmpty(document.getContent()) && !reload) {
            return ResolveResult.ok(document.getContent());
        }

        Document.ApifoxConfig apiConfig = document.getApifoxConfig();
        if (apiConfig == null) {
            return ResolveResult.fail("invalid document config");
        }
        Connection connection = ConnectionRepository.getInstance().getWithCredential(apiConfig.getConnectionId());
        String serviceUrl = connection != null ? connection.getBaseUrl() : apiConfig.getServiceUrl();
        String accessToken = connection != null ? connection.getCredential() : apiConfig.getAccessToken();

        boolean invalid = StringUtils.isAnyEmpty(serviceUrl, accessToken, apiConfig.getProjectId());
        if (invalid) {
            return ResolveResult.fail("invalid apifox config");
        }
        String projectId = apiConfig.getProjectId();

        String url = serviceUrl + String.format(EXPORT_PATH, projectId);
        ExportRequest request = new ExportRequest();
        byte[] body = JsonUtils.toJson(request).getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Apifox-Api-Version", "2024-03-28");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + accessToken);
        try {
            byte[] data = HttpUtils.post(url, body, headers, Duration.ofSeconds(10));
            return ResolveResult.ok(new String(data, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResolveResult.fail("download file failed: " + ExceptionUtils.getMessage(e));
        }
    }

    @Data
    private static class ExportRequest {

        private Scope scope = new Scope();

        private String oasVersion = "3.0";

        private String exportFormat = "json";

        private List<String> environmentIds;

        @Data
        public static class Scope {
            private String type = "all";
            private List<String> excludedByTags;
        }
    }

}
