package io.apicopilot.codegen.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 代码生成原始配置
 */
@Data
public class GenerateConfig {

    /**
     * 语言信息
     */
    private List<Language> languages;

    /**
     * 生成模型
     */
    private Model model;

    /**
     * 生成请求
     */
    private Request request;

    /**
     * 生成代码
     */
    private Code code;

    @Data
    public static class Model {

        /**
         * 支持的语言
         */
        private List<String> languages;

        /**
         * 模板配置
         */
        private List<ModelTemplate>  templates;
    }

    @Data
    public static class Request {

        /**
         * 支持的语言
         */
        private List<String> languages;

        /**
         * 模板
         */
        private List<RequestTemplate> templates;
    }

    @Data
    public static class Code {

        /**
         * 支持的语言
         */
        private List<String> languages;

        /**
         * 模板配置
         */
        private List<CodeTemplate>  templates;
    }

    public String getExtension(@NotNull String language) {
        Language lang = languages.stream().filter(o -> o.getLanguage().equalsIgnoreCase(language)).findFirst().orElse(null);
        return Objects.nonNull(lang) ? lang.getExtension() : null;
    }

    public List<String> getModelLanguages() {
        if (model == null) {
            return Collections.emptyList();
        }
        if (model.languages != null) {
            return model.languages;
        }
        return model.getTemplates().stream().map(ModelTemplate::getLanguage).distinct().collect(Collectors.toList());
    }

    public ModelTemplate getModelTemplate(@NotNull String language) {
        if (model == null || model.getTemplates() == null) {
            return null;
        }
        return model.getTemplates().stream().filter(o -> o.getLanguage().equalsIgnoreCase(language)).findFirst().orElse(null);
    }

    public List<String> getRequestLanguages() {
        if (request == null) {
            return Collections.emptyList();
        }
        if (request.languages != null) {
            return request.languages;
        }
        return request.getTemplates().stream().map(RequestTemplate::getLanguage).distinct().collect(Collectors.toList());
    }

    public List<RequestTemplate> getRequestTemplates(@NotNull String language) {
        if (request == null || request.getTemplates() == null) {
            return Collections.emptyList();
        }
        return request.getTemplates().stream().filter(o -> o.getLanguage().equalsIgnoreCase(language)).collect(Collectors.toList());
    }

    public List<String> getCodeLanguages() {
        if (code == null) {
            return Collections.emptyList();
        }
        if (code.languages != null) {
            return Collections.emptyList();
        }
        return code.getTemplates().stream().map(CodeTemplate::getLanguage).distinct().collect(Collectors.toList());
    }

    public List<CodeTemplate> getCodeTemplates(@NotNull String language) {
        if (code == null || code.getTemplates() == null) {
            return Collections.emptyList();
        }
        return code.getTemplates().stream().filter(o -> o.getLanguage().equalsIgnoreCase(language)).collect(Collectors.toList());
    }

    public CodeTemplate getCodeTemplate(@NotNull String language, String name) {
        if (code == null || code.getTemplates() == null) {
            return null;
        }
        return code.getTemplates().stream().filter(o -> o.getLanguage().equalsIgnoreCase(language) && o.getName().equals(name)).findFirst().orElse(null);
    }
}
