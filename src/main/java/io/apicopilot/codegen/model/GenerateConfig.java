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

    @Data
    public static class Model {

        /**
         * 支持的语言
         */
        private List<String> languages;
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

    public String getExtension(@NotNull String language) {
        Language lang = languages.stream().filter(o -> o.getLanguage().equalsIgnoreCase(language)).findFirst().orElse(null);
        return Objects.nonNull(lang) ? lang.getExtension() : null;
    }

    public List<String> getModelLanguages() {
        if (model == null || model.getLanguages() == null) {
            return Collections.emptyList();
        }
        return model.getLanguages();
    }

    public List<String> getRequestLanguages() {
        if (request == null) {
            return Collections.emptyList();
        }
        if (request.languages != null) {
            return Collections.emptyList();
        }
        return request.getTemplates().stream().map(RequestTemplate::getLanguage).distinct().collect(Collectors.toList());
    }

    public List<RequestTemplate> getRequestTemplates(@NotNull String language) {
        if (request == null || request.getTemplates() == null) {
            return Collections.emptyList();
        }
        return request.getTemplates().stream().filter(o -> o.getLanguage().equalsIgnoreCase(language)).collect(Collectors.toList());
    }
}
