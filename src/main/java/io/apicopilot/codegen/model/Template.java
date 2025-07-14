package io.apicopilot.codegen.model;

import lombok.Data;

import java.util.List;

/**
 * 模板配置
 */
@Data
public class Template {

    /**
     * 语言
     */
    private String language;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 生成文件
     */
    private List<TemplateFile> files;

    @Data
    public static class TemplateFile {

        /**
         * 生成文件名
         */
        private String filename;

        /**
         * 模板文件内容
         */
        private String template;
    }
}
