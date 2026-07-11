package io.apix.codegen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代码生成模板配置
 */
@Data
public class CodeTemplate {

    private String language;
    private String name;
    private List<Option> options;
    private List<File> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String label;
        private String name;
        private String type;
        private Boolean required;
        private Object defaultValue;
        private List<Value> values;
        private Boolean hidden;
    }


    @Data
    public static class Value {
        private String value;
    }

    @Data
    public static class File {
        private String template;
        private String type;
        private String extension;
        private String path;
    }
}
