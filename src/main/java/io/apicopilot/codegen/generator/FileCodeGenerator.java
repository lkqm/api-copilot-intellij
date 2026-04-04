package io.apicopilot.codegen.generator;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.intellij.openapi.project.Project;
import io.apicopilot.codegen.context.*;
import io.apicopilot.codegen.core.ApiModelGenerator;
import io.apicopilot.codegen.core.GenerateConfigs;
import io.apicopilot.codegen.core.TypeMappings;
import io.apicopilot.codegen.core.TypeResolverImpl;
import io.apicopilot.codegen.model.ApiModel;
import io.apicopilot.codegen.model.CodeFile;
import io.apicopilot.codegen.model.CodeTemplate;
import io.apicopilot.codegen.model.PropertyModel;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.HandlebarsUtils;
import io.apicopilot.util.OpenApiTagResolver;
import io.apicopilot.util.ResourceUtils;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileCodeGenerator {
    private final Project project;
    private final Document document;
    private final List<Request> requests;
    private final Map<String, Template> templates = new ConcurrentHashMap<>();

    public FileCodeGenerator(Project project, Document document, List<Request> requests) {
        this.project = project;
        this.document = document;
        this.requests = requests;
    }

    @SneakyThrows
    public List<CodeFile> generate(String language, String type, Map<String, Object> options) {
        // response handle mode
        String responseHandleDataField = options.getOrDefault("responseHandleDataField", "").toString();
        if(StringUtils.isEmpty(responseHandleDataField)) {
            options.put("responseHandleDataField", "raw");
        }

        String responseHandleMode = options.getOrDefault("responseHandleMode", "raw").toString();
        if ("unwrap".equals(responseHandleMode)) {
            options.put("responseHandleModeUnwrap", true);
        } else if ("wrap".equals(responseHandleMode)) {
            options.put("responseHandleModeWrap", true);
        } else {
            options.put("responseHandleModeRaw", true);
        }

        CodeTemplate template = GenerateConfigs.getInstance().getConfig().getCodeTemplate(language, type);
        TypeMappings typeMappings = TypeMappings.getInstance();
        List<ApiModel> apis = requests.stream().map(request -> {
            ApiModelGenerator modelGenerator = new ApiModelGenerator(request, language, new TypeResolverImpl(language, typeMappings.get(language)));
            ApiModel apiModel = modelGenerator.get();
            
            // response handle
            PropertyModel responseBody = apiModel.getResponseBody();
            if(!"raw".equals(responseHandleMode) && responseBody != null && responseBody.getProperties() != null) {
                PropertyModel dataModel = responseBody.getProperties().stream().filter(o -> responseHandleDataField.equals(o.getName())).findFirst().orElse(null);
                responseBody.setDataModel(dataModel);

                if(apiModel.getModels() != null) {
                    apiModel.getModels().forEach(model -> {
                        if(BooleanUtils.isTrue(model.getIsResponseModel())) {
                            model.setIsSkip(true);
                        }
                    });
                    List<PropertyModel> models = apiModel.getModels().stream().filter(m -> BooleanUtils.isNotTrue(m.getIsSkip())).collect(Collectors.toList());
                    apiModel.setModels(models);
                }
            }

            return apiModel;
        }).collect(Collectors.toList());
        Map<String, List<ApiModel>> tagToApis = apis.stream()
                .collect(Collectors.groupingBy(api -> CollectionUtils.isEmpty(api.getTags()) ? "" : api.getTags().get(0)));

        List<CodeFile> codeFiles = new ArrayList<>();
        List<CodeTemplate.File> files = template.getFiles();
        Map<String, Map<String, Object>> optionsCache = new HashMap<>();
        Map<String, String> tagsCache = new HashMap<>();

        for (CodeTemplate.File file : files) {
            Template fileTemplate = getTemplate(file.getTemplate());

            if ("client".equals(file.getType()) || "models".equals(file.getType())) {
                PathContext pathContext = PathContext.builder()
                        .doc(document.getName())
                        .build();
                pathContext.putAll(options);
                String path = HandlebarsUtils.apply(file.getPath(), pathContext);
                boolean multiple = isMultiplePath(path);
                if (multiple) {
                    Map<String, String> tagPaths = new HashMap<>();
                    for (Map.Entry<String, List<ApiModel>> entry : tagToApis.entrySet()) {
                        String tag = getTag(tagsCache, entry.getKey());
                        List<ApiModel> tagApis = entry.getValue();
                        Map<String, Object> tagOperations = getTagOptions(optionsCache, tag, options);
                        ClientContext context = ClientContext.builder()
                                .apis(tagApis)
                                .models(tagApis.stream().map(ApiModel::getModels).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList()))
                                .build();
                        context.putAll(tagOperations);
                        String tagPath = getTagPath(tagPaths, tag, path);
                        String code = fileTemplate.apply(context);
                        codeFiles.add(new CodeFile(file.getType(), tagPath, code));
                    }
                } else {
                    String filePath = HandlebarsUtils.apply(path, pathContext);
                    ClientContext context = ClientContext.builder()
                            .apis(apis)
                            .models(apis.stream().map(ApiModel::getModels).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList()))
                            .build();
                    context.putAll(options);
                    String code = fileTemplate.apply(context);
                    codeFiles.add(new CodeFile(file.getType(), filePath, code));
                }
            } else if ("model".equals(file.getType())) {
                for (ApiModel api : apis) {
                    String tag = getTag(tagsCache, api);
                    Map<String, Object> tagOperations = getTagOptions(optionsCache, tag, options);
                    ModelContext context = new ModelContext();
                    context.putAll(tagOperations);

                    for (PropertyModel model : api.getModels()) {
                        context.setTag(CollectionUtils.isNotEmpty(api.getTags()) ? api.getTags().get(0) : "");
                        context.setModel(model);
                        context.setModels(Collections.singletonList(model));
                        context.setModelName(model.getTargetType());
                        context.putAll(tagOperations);
                        String path = HandlebarsUtils.apply(file.getPath(), context);
                        path = HandlebarsUtils.apply(path, context);
                        String code = fileTemplate.apply(context);
                        codeFiles.add(new CodeFile(file.getType(), path, code));
                    }
                }
            }
        }

        // handle latest file
        List<LastestContext.File> outputFiles = codeFiles.stream().map(f -> LastestContext.File.builder().type(f.getType()).path(f.getPath()).build()).collect(Collectors.toList());
        List<CodeTemplate.File> latestFiles = files.stream().filter(f -> "latest".equals(f.getType())).collect(Collectors.toList());
        for (CodeTemplate.File file : latestFiles) {
            // output file path
            PathContext pathContext = PathContext.builder()
                    .doc(document.getName())
                    .build();
            pathContext.putAll(options);
            String path = HandlebarsUtils.apply(file.getPath(), pathContext);
            if (StringUtils.isNotEmpty(path)) {
                path = HandlebarsUtils.apply(path, pathContext);
            }
            if (StringUtils.isEmpty(path)) {
                continue;
            }

            Template fileTemplate = getTemplate(file.getTemplate());
            LastestContext context = new LastestContext();
            context.setFiles(outputFiles);
            String code = fileTemplate.apply(context);
            codeFiles.add(new CodeFile(file.getType(), path, code));
        }


        return codeFiles;
    }


    private Template getTemplate(String path) {
        return templates.computeIfAbsent(path, (key) -> {
            try {
                Handlebars handlebars = HandlebarsUtils.create();
                String templateContent = ResourceUtils.readAsTextWithCache(path);
                return handlebars.compileInline(templateContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean isMultiplePath(String path) {
        PathContext context = PathContext.builder()
                .doc("doc")
                .tag("tag")
                .build();
        String path1 = HandlebarsUtils.apply(path, context);
        context.setTag("gat");
        String path2 = HandlebarsUtils.apply(path, context);
        return !path1.equals(path2);
    }

    private Map<String, Object> getTagOptions(Map<String, Map<String, Object>> cache, String tag, Map<String, Object> options) {
        return cache.computeIfAbsent(tag, k -> {
            OptionContext context = OptionContext.builder()
                    .doc(document.getName())
                    .tag(tag)
                    .build();
            Map<String, Object> result = new HashMap<>();
            options.forEach((key, value) -> {
                if (value instanceof String) {
                    String optionValue = HandlebarsUtils.apply(value.toString(), context);
                    result.put(key, optionValue);
                } else {
                    result.put(key, value);
                }
            });
            return result;
        });
    }

    private String getTagPath(Map<String, String> cache, String tag, String path) {
        return cache.computeIfAbsent(tag + ":" + path, k -> {
            PathContext context = PathContext.builder()
                    .doc(document.getName())
                    .tag(tag)
                    .build();
            return HandlebarsUtils.apply(path, context);
        });

    }

    private String getTag(Map<String, String> cache, ApiModel api) {
        String tag = CollectionUtils.isEmpty(api.getTags()) ? "" : api.getTags().get(0);
        return getTag(cache, tag);
    }

    private String getTag(Map<String, String> cache, String tag) {
        return cache.computeIfAbsent(tag, k -> {
            String tagToUse = OpenApiTagResolver.inferResourceName(document.getOpenApi(), tag);
            if (tagToUse != null) {
                tagToUse = tagToUse.toLowerCase();
            }
            return tagToUse;
        });
    }

}

