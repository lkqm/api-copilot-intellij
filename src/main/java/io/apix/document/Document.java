package io.apix.document;

import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.OptionTag;
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
     * 是否自动更新
     */
    private boolean autoSyncEnabled;

    /**
     * 是否导出到项目目录 .apis/
     */
    private boolean exportEnabled;

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

    /**
     * 最后导出成功时间
     */
    private Long exportLastSuccessTime;

    /**
     * 最后导出失败消息
     */
    private String exportLastFailMessage;

    /**
     * 是否有远程更新
     */
    private boolean hasUpdate;

    /**
     * 最后检测时间
     */
    private Long lastCheckTime;

    @Transient
    public OpenAPI getOpenApi() {
        return openApi;
    }

    public boolean isRemoteDocument() {
        if (type == DocumentSourceType.Apifox || type == DocumentSourceType.SwaggerHub) {
            return true;
        }
        return type == DocumentSourceType.OpenAPI && openApiConfig != null && openApiConfig.isRemote();
    }

    /**
     * Create a duplicate with a new id and modified name.
     */
    public Document duplicate() {
        Document copy = new Document();
        copy.setName(this.name);
        copy.setEnable(true);
        copy.setType(this.type);
        copy.setOpenApiConfig(this.openApiConfig);
        copy.setApifoxConfig(this.apifoxConfig);
        copy.setSwaggerHubConfig(this.swaggerHubConfig);
        copy.setAutoSyncEnabled(this.autoSyncEnabled);
        copy.setExportEnabled(this.exportEnabled);
        // id is null so save() will generate a new UUID
        return copy;
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
        @Transient
        private String accessToken;

        /**
         * Compatibility field for tokens stored by older versions.
         */
        @OptionTag("accessToken")
        private String legacyAccessToken;

        /**
         * 项目id
         */
        private String projectId;

        @Transient
        public String getAccessToken() {
            return accessToken;
        }

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
        @Transient
        private String apiKey;

        /**
         * Compatibility field for keys stored by older versions.
         */
        @OptionTag("apiKey")
        private String legacyApiKey;

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

        @Transient
        public String getApiKey() {
            return apiKey;
        }

    }
}
