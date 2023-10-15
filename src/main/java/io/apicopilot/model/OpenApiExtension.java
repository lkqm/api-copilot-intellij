package io.apicopilot.model;

import com.google.gson.Gson;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;

/**
 * OpenAPI extends for apilot.
 *
 * @see io.swagger.v3.oas.models.OpenAPI
 */
@Data
public class OpenApiExtension {

    public static final String KEY = "x-apilot-openapi";

    /**
     * 文档id
     */
    private String documentId;

    /**
     * 文档名
     */
    private String documentName;

    /**
     * 三方文档id（项目id）
     */
    private String thirdDocumentId;

    public static void set(OpenAPI operation, OpenApiExtension extension) {
        operation.addExtension(KEY, extension);
    }

    public static OpenApiExtension get(OpenAPI openApi) {
        if (openApi == null || openApi.getExtensions() == null || openApi.getExtensions().isEmpty()) {
            return null;
        }
        Object o = openApi.getExtensions().get(KEY);
        if (o == null) {
            return null;
        }

        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(o), OpenApiExtension.class);
    }
}
