package io.apicopilot.codegen.generator;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.apicopilot.codegen.core.ApiModelGenerator;
import io.apicopilot.codegen.core.TypeMappings;
import io.apicopilot.codegen.core.TypeResolverImpl;
import io.apicopilot.codegen.model.ApiModel;
import io.apicopilot.codegen.model.GenerateModelsContext;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.HandlebarsUtils;
import io.apicopilot.util.ResourceUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelCodeGenerator {

    private final Document document;
    private final Request request;
    private final Map<String, Template> templates = new ConcurrentHashMap<>();

    public ModelCodeGenerator(Document document, Request request) {
        this.document = document;
        this.request = request;
    }

    @SneakyThrows
    public String generateCode(String language) {
        TypeMappings typeMappings = TypeMappings.getInstance();
        ApiModelGenerator modelGenerator = new ApiModelGenerator(request, language, new TypeResolverImpl(language, typeMappings.get(language)));
        ApiModel apiModel = modelGenerator.get();

        Template template = getTemplate(language);
        return template.apply(GenerateModelsContext.builder()
                .models(apiModel.getModels())
                .build());
    }

    private Template getTemplate(String language) {
        return templates.computeIfAbsent(language, (key) -> {
            try {
                Handlebars handlebars = HandlebarsUtils.create();
                String templateContent = ResourceUtils.readAsTextWithCache("codegen/model/" + language.toLowerCase() + ".hbs");
                return handlebars.compileInline(templateContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
