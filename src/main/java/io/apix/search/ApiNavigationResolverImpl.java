package io.apix.search;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import io.apix.document.Document;
import io.apix.document.DocumentManager;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ApiNavigationResolverImpl implements ApiNavigationResolver {

    private final Project project;

    @Override
    public @NotNull List<ApiNavigationData> getApis() {
        DocumentManager manager = DocumentManager.getInstance(project);
        List<Document> documents = manager.getDocuments();

        List<ApiNavigationData> apis = new ArrayList<>();
        for (Document document : documents) {
            if (document != null && document.getOpenApi() != null && document.isEnable()) {
                apis.addAll(resolveApis(document.getId(), document.getName(), document.getOpenApi()));
            }
        }
        return apis;
    }

    @SneakyThrows
    private List<ApiNavigationData> resolveApis(String documentId, String documentName, OpenAPI openApi) {
        List<ApiNavigationData> apis = Lists.newArrayList();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                ApiNavigationData api = ApiNavigationData.builder()
                        .documentId(documentId)
                        .documentName(documentName)
                        .path(path)
                        .method(method.name())
                        .summary(operation.getSummary())
                        .description(operation.getDescription())
                        .deprecated(operation.getDeprecated())
                        .build();
                apis.add(api);
            });
        });
        return apis;
    }

}
