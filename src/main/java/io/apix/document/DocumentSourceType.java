package io.apix.document;

import com.intellij.openapi.project.Project;
import io.apix.document.resolver.ApiResolver;
import io.apix.document.resolver.ApifoxApiResolver;
import io.apix.document.resolver.OpenAPIApiResolver;
import io.apix.document.resolver.SwaggerHubApiResolver;
import io.apix.window.dialog.ApifoxDocumentEditForm;
import io.apix.window.dialog.DocumentEditForm;
import io.apix.window.dialog.OpenApiDocumentEditForm;
import io.apix.window.dialog.SwaggerHubDocumentEditForm;

public enum DocumentSourceType {
    OpenAPI, Apifox, SwaggerHub;

    public static ApiResolver getApiResolver(DocumentSourceType type, Project project, Document document) {
        if (type == DocumentSourceType.OpenAPI) {
            return new OpenAPIApiResolver(project, document);
        } else if (type == DocumentSourceType.Apifox) {
            return new ApifoxApiResolver(document);
        } else if (type == DocumentSourceType.SwaggerHub) {
            return new SwaggerHubApiResolver(document);
        }
        return null;
    }

    public static DocumentEditForm getDocumentEditForm(DocumentSourceType type) {
        if (type == DocumentSourceType.OpenAPI) {
            return new OpenApiDocumentEditForm();
        } else if (type == DocumentSourceType.Apifox) {
            return new ApifoxDocumentEditForm();
        } else if (type == DocumentSourceType.SwaggerHub) {
            return new SwaggerHubDocumentEditForm();
        }
        return null;
    }
}
