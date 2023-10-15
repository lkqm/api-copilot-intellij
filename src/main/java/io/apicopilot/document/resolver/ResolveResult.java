package io.apicopilot.document.resolver;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;

/**
 * Resolve result.
 */
@Data
public class ResolveResult {

    /**
     * 解析是否成功
     */
    private boolean success;

    /**
     * 文档内容
     */
    private String openApiContent;

    /**
     * 文档
     */
    private OpenAPI openApi;

    /**
     * 失败原因
     */
    private String failReason;

    public static ResolveResult ok(String openApiContent) {
        ResolveResult resolveResult = new ResolveResult();
        resolveResult.setSuccess(true);
        resolveResult.setOpenApiContent(openApiContent);
        return resolveResult;
    }

    public static ResolveResult ok(String openApiContent, OpenAPI openApi) {
        ResolveResult resolveResult = new ResolveResult();
        resolveResult.setSuccess(true);
        resolveResult.setOpenApiContent(openApiContent);
        resolveResult.setOpenApi(openApi);
        return resolveResult;
    }

    public static ResolveResult fail(String reason) {
        ResolveResult resolveResult = new ResolveResult();
        resolveResult.setSuccess(false);
        resolveResult.setFailReason(reason);
        return resolveResult;
    }
}
