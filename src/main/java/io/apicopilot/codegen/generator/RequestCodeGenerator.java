package io.apicopilot.codegen.generator;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.apicopilot.codegen.core.ApiModelGenerator;
import io.apicopilot.codegen.core.GenerateConfigs;
import io.apicopilot.codegen.core.TypeMappings;
import io.apicopilot.codegen.core.TypeResolverImpl;
import io.apicopilot.codegen.model.ApiModel;
import io.apicopilot.codegen.model.RequestTemplate;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.HandlebarsUtils;
import io.apicopilot.util.ResourceUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestCodeGenerator {

    private final Document document;
    private final Request request;
    private final Map<String, Template> templates = new ConcurrentHashMap<>();

    public RequestCodeGenerator(Document document, Request request) {
        this.document = document;
        this.request = request;
    }

    @SneakyThrows
    public String generateCode(String language, String name) {
        TypeMappings typeMappings = TypeMappings.getInstance();
        ApiModelGenerator modelGenerator = new ApiModelGenerator(request, language, new TypeResolverImpl(language, typeMappings.get(language)));
        ApiModel apiModel = modelGenerator.get();

        Template template = getTemplate(language, name);
        if (template == null) {
            return "";
        }

        return template.apply(apiModel);
    }

    private Template getTemplate(String language, String name) {
        return templates.computeIfAbsent(language + ":" + name, (key) -> {
            try {
                RequestTemplate requestTemplate = GenerateConfigs.getInstance().getConfig().getRequestTemplates(language).stream().filter(t -> name.equals(t.getName())).findFirst().orElse(null);
                if (requestTemplate == null) {
                    return null;
                }
                Handlebars handlebars = HandlebarsUtils.create();
                String templateContent = ResourceUtils.readAsTextWithCache(requestTemplate.getTemplate());
                return handlebars.compileInline(templateContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
