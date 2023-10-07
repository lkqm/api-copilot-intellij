package apicopilot.search;

import apicopilot.apidoc.DocumentProvider;
import apicopilot.apidoc.FileDocumentProvider;
import apicopilot.model.Document;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ApiNavigationResolverImpl implements ApiNavigationResolver {

    private final Project project;


    @Override
    public @NotNull List<ApiNavigationData> getApis() {
        DocumentProvider documentProvider = new FileDocumentProvider(project);
        Document document = documentProvider.getDocument();
        if (document != null && document.getOpenApi() != null) {
            return resolveApis(document.getOpenApi());
        }
        return Collections.emptyList();
    }

    @SneakyThrows
    private List<ApiNavigationData> resolveApis(OpenAPI openApi) {
        List<ApiNavigationData> apis = Lists.newArrayList();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                ApiNavigationData api = ApiNavigationData.builder()
                        .connectionId("default")
                        .path(path)
                        .method(method.name())
                        .summary(operation.getSummary())
                        .description(operation.getDescription())
                        .build();
                apis.add(api);
            });
        });
        return apis;
    }

}
