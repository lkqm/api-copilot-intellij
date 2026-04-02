package io.apicopilot.document.resolver;

import io.apicopilot.document.Document;
import io.apicopilot.util.HttpUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolve API document from OpenAPI.
 */
@AllArgsConstructor
public class SwaggerHubApiResolver extends AbstractApiResolver {

    private final Document document;

    public static final String EXPORT_PATH = "/apis/%s/%s/%s";

    @SneakyThrows
    @Override
    protected ResolveResult getContent(boolean reload) {
        if (StringUtils.isNotEmpty(document.getContent()) && !reload) {
            return ResolveResult.ok(document.getContent());
        }

        Document.SwaggerHubConfig apiConfig = document.getSwaggerHubConfig();
        if (apiConfig == null) {
            return ResolveResult.fail("invalid document config");
        }
        boolean invalid = StringUtils.isAnyEmpty(apiConfig.getServiceUrl(), apiConfig.getApiKey(), apiConfig.getOwner(), apiConfig.getApi());
        if (invalid) {
            return ResolveResult.fail("invalid swagger hub config");
        }

        String url = apiConfig.getServiceUrl() + String.format(EXPORT_PATH, apiConfig.getOwner(), apiConfig.getApi(), apiConfig.getVersion());
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiConfig.getApiKey());
        headers.put("Accept", "application/json");
        try {
            byte[] data = HttpUtils.get(url, headers, Duration.ofSeconds(10));
            return ResolveResult.ok(new String(data, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResolveResult.fail("download file failed: " + ExceptionUtils.getMessage(e));
        }
    }

}
