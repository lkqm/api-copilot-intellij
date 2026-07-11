package io.apix.debug;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug HTTP request model.
 */
@Data
public class DebugHttpRequest {

    private String method;
    private String baseUrl;
    private String path;
    private Map<String, String> pathParams = new LinkedHashMap<>();
    private List<QueryParam> queryParams = new ArrayList<>();
    private Map<String, String> headers = new LinkedHashMap<>();
    private String body;
    private byte[] binaryBody;

    /** Set by AuthConfig when Digest auth is configured. Used by DebugHttpClient for challenge-response. */
    private String digestUsername;
    private String digestPassword;

    @Data
    public static class QueryParam {
        private boolean enabled = true;
        private String name;
        private String value = "";

        public QueryParam() {
        }

        public QueryParam(boolean enabled, String name, String value) {
            this.enabled = enabled;
            this.name = name;
            this.value = value != null ? value : "";
        }
    }
}
