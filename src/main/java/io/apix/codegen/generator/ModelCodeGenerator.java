package io.apix.codegen.generator;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.apix.codegen.context.ModelsContext;
import io.apix.codegen.core.ApiModelGenerator;
import io.apix.codegen.core.TypeMappings;
import io.apix.codegen.core.TypeResolverImpl;
import io.apix.codegen.model.ApiModel;
import io.apix.codegen.model.GenerateModelTarget;
import io.apix.codegen.model.PropertyModel;
import io.apix.document.Document;
import io.apix.model.Request;
import io.apix.util.HandlebarsUtils;
import io.apix.util.ResourceUtils;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelCodeGenerator {

    private final Document document;
    private final Request request;
    private final GenerateModelTarget target;
    private final Map<String, Template> templates = new ConcurrentHashMap<>();

    public ModelCodeGenerator(Document document, Request request) {
        this(document, request, GenerateModelTarget.all());
    }

    public ModelCodeGenerator(Document document, Request request, GenerateModelTarget target) {
        this.document = document;
        this.request = request;
        this.target = target != null ? target : GenerateModelTarget.all();
    }

    @SneakyThrows
    public String generateCode(String language, Map<String, Object> options) {
        TypeMappings typeMappings = TypeMappings.getInstance();
        TypeResolverImpl typeResolver = new TypeResolverImpl(language, typeMappings.get(language));
        ApiModelGenerator modelGenerator = new ApiModelGenerator(request, language, typeResolver);
        ApiModel apiModel = modelGenerator.get();
        List<PropertyModel> models = resolveModels(apiModel, typeResolver);
        if (CollectionUtils.isEmpty(models)) {
            return "";
        }

        Template template = getTemplate(language);
        ModelsContext context = new ModelsContext();
        context.setModels(models);
        context.putAll(options);
        return template.apply(context);
    }

    private List<PropertyModel> resolveModels(ApiModel apiModel, TypeResolverImpl typeResolver) {
        if (target.getScope() == GenerateModelTarget.Scope.CUSTOM_SCHEMA) {
            return collectModels(PropertyModel.of(target.getSchema(), false, typeResolver, target.getDefaultTypeName()));
        }
        return apiModel.getModels();
    }

    private List<PropertyModel> collectModels(PropertyModel root) {
        if (root == null) {
            return List.of();
        }
        List<PropertyModel> models = new java.util.ArrayList<>();
        if (Boolean.TRUE.equals(root.getIsObject()) && CollectionUtils.isNotEmpty(root.getProperties())) {
            models.add(root);
        } else if (Boolean.TRUE.equals(root.getIsArray())
                && root.getItems() != null
                && Boolean.TRUE.equals(root.getItems().getIsObject())
                && CollectionUtils.isNotEmpty(root.getItems().getProperties())) {
            models.add(root.getItems());
        }
        models.addAll(root.flatObjectModels());
        return models;
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
