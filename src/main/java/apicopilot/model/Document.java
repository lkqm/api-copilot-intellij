package apicopilot.model;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;

/**
 * API Document.
 */
@Data
public class Document {

    /**
     * OpenApi.
     */
    private OpenAPI openApi;

    private OpenApiExtension extension;

    public void setOpenApi(OpenAPI openApi) {
        this.openApi = openApi;
        this.extension = OpenApiExtension.get(openApi);
    }

    public String getId() {
        return extension != null ? extension.getDocumentId() : null;
    }

    public String getName() {
        return extension != null ? extension.getDocumentName() : null;
    }
}
