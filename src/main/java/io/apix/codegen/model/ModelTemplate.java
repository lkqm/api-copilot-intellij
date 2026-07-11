package io.apix.codegen.model;

import lombok.Data;

import java.util.List;

/**
 * 代码生成模板配置
 */
@Data
public class ModelTemplate {

    private String language;
    private List<Option> options;
    private List<File> files;

    @Data
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
        private String name;
        private String template;
    }
}
