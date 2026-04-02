package io.apicopilot.document;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import io.apicopilot.document.resolver.ApiResolver;
import io.apicopilot.document.resolver.ResolveResult;
import io.apicopilot.document.topic.DocumentTopic;
import io.apicopilot.model.Api;
import io.apicopilot.model.Request;
import io.apicopilot.util.NotificationUtils;
import io.apicopilot.util.PathUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Documents manager.
 */
@RequiredArgsConstructor
public class DocumentManager {

    private final Project project;

    public static DocumentManager getInstance(@NotNull Project project) {
        return new DocumentManager(project);
    }

    /**
     * 获取所有文档
     */
    @NotNull
    public List<Document> getDocuments() {
        DocumentSettings settings = DocumentSettings.getInstance(this.project);
        List<Document> documents = settings.getDocuments();
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        documents.forEach(document -> {
            if (document.getOpenApi() == null && StringUtils.isNotEmpty(document.getContent())) {
                ParseOptions parseOptions = new ParseOptions();
                parseOptions.setResolveFully(true);
                SwaggerParseResult parseResult = new OpenAPIParser().readContents(document.getContent(), null, parseOptions);
                document.setOpenApi(parseResult.getOpenAPI());
            }
        });
        return documents;
    }

    /**
     * 重新加载文档
     */
    public LoadResult reloadDocument(Document document) {
        LoadResult result = doLoadDocument(document, true);

        // publish
        DocumentTopic topic = project.getMessageBus().syncPublisher(DocumentTopic.TOPIC);
        topic.onLoaded(document, result);
        return result;
    }

    private LoadResult doLoadDocument(Document document, boolean reload) {
        DocumentSourceType type = document.getType();
        ApiResolver apiResolver = DocumentSourceType.getApiResolver(type, project, document);
        ResolveResult resolveResult = apiResolver != null ? apiResolver.resolve(reload) : ResolveResult.fail("Unknown document type: " + type);

        boolean changed = false;
        if (resolveResult.isSuccess()) {
            changed = !Objects.equals(document.getContent(), resolveResult.getOpenApiContent());
            document.setContent(resolveResult.getOpenApiContent());
            document.setOpenApi(resolveResult.getOpenApi());
            document.setLoadTime(System.currentTimeMillis());
        }
        document.setLoading(false);
        DocumentRepository.getInstance(this.project).save(document);

        return LoadResult.builder()
                .success(resolveResult.isSuccess())
                .failReason(resolveResult.getFailReason())
                .changed(changed)
                .build();
    }

    /**
     * 搜索API
     *
     * @param path   路径
     * @param method 方法
     * @return 匹配的接口
     */
    public List<Api> getApi(@NotNull String path, @Nullable String method) {
        List<Api> apis = Lists.newArrayList();

        List<Document> documents = this.getDocuments();
        for (Document document : documents) {
            if (document == null || document.getOpenApi() == null) {
                continue;
            }
            List<Request> requests = resolveRequests(document.getOpenApi(), path, method);
            requests.forEach(request -> {
                apis.add(new Api(document, request));
            });
        }
        return apis;
    }

    private List<Request> resolveRequests(OpenAPI openApi, String targetPath, String targetMethod) {
        List<Request> requests = Lists.newArrayList();
        List<Request> exactRequests = Lists.newArrayList();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                if ((targetMethod != null && !method.name().equalsIgnoreCase(targetMethod))) {
                    return;
                }
                PathUtils.MatchResult match = PathUtils.isMatch(path, targetPath);
                if (match.isMatched()) {
                    Request request = new Request(path, method.name(), operation);
                    requests.add(request);
                    if (match.isExact()) {
                        exactRequests.add(request);
                    }
                }
            });
        });
        return !exactRequests.isEmpty() ? exactRequests : requests;
    }

}
