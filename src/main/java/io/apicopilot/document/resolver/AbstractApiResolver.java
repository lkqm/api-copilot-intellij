package io.apicopilot.document.resolver;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * ApiResolve templates.
 */
@AllArgsConstructor
public abstract class AbstractApiResolver implements ApiResolver {

    @NotNull
    @Override
    public ResolveResult resolve(boolean refresh) {
        ResolveResult result = getContent(refresh);
        if (!result.isSuccess()) {
            return result;
        }
        if (StringUtils.isEmpty(result.getOpenApiContent())) {
            return ResolveResult.fail("invalid OpenAPI content");
        }
        SwaggerParseResult swaggerResult = doParseOpenApi(result.getOpenApiContent());
        if (swaggerResult.getOpenAPI() == null) {
            return ResolveResult.fail("invalid OpenAPI content");
        }
        result.setOpenApi(swaggerResult.getOpenAPI());
        return result;
    }

    protected abstract ResolveResult getContent(boolean reload);

    private SwaggerParseResult doParseOpenApi(String content) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);
        return new OpenAPIParser().readContents(content, null, parseOptions);
    }
}
