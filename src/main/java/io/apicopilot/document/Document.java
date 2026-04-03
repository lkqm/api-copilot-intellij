package io.apicopilot.document;

import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;

/**
 * Document.
 */
@Data
public class Document {

    /**
     * 文档id
     */
    private String id;

    /**
     * 文档名
     */
    private String name;

    /**
     * 是否启用
     */
    private boolean enable = true;

    /**
     * 文档内容
     */
    @Tag("content")
    private String content;

    /**
     * 文档类型
     */
    private DocumentSourceType type;

    /**
     * OpenAPI来源信息
     */
    private OpenApiConfig openApiConfig;

    /**
     * Apifox信息配置
     */
    private ApifoxConfig apifoxConfig;

    /**
     * SwaggerHub信息配置
     */
    private SwaggerHubConfig swaggerHubConfig;

    /**
     * 文档对象
     */
    @Transient
    private OpenAPI openApi;

    /**
     * 同步状态
     */
    private SyncStatus syncStatus;

    /**
     * 最后同步成功时间
     */
    private Long lastSuccessTime;

    /**
     * 最后同步失败消息
     */
    private String lastFailMessage;

    @Transient
    public OpenAPI getOpenApi() {
        return openApi;
    }

    @Transient
    public boolean isLoaded() {
        return content != null;
    }

    /**
     * OpenAPI来源配置
     */
    @Data
    public static class OpenApiConfig {

        /**
         * OpenAPI文件路径
         */
        private String path;

        public boolean isRemote() {
            return path != null && (path.startsWith("http://") || path.startsWith("https://"));
        }
    }

    /**
     * Apifox来源配置
     */
    @Data
    public static class ApifoxConfig {
        /**
         * 服务地址
         */
        private String serviceUrl = "https://api.apifox.com";

        /**
         * 访问令牌
         */
        private String accessToken;

        /**
         * 项目id
         */
        private String projectId;

    }

    /**
     * SwaggerHub来源配置
     */
    @Data
    public static class SwaggerHubConfig {
        /**
         * 服务地址
         */
        private String serviceUrl = "https://api.swaggerhub.com";

        /**
         * 访问令牌
         */
        private String apiKey;

        /**
         * 用户和组织
         */
        private String owner;

        /**
         * 文档标识
         */
        private String api;

        /**
         * 文档版本
         */
        private String version = "default";

    }
}
