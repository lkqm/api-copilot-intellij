package io.apicopilot.codegen.model;

import lombok.Data;

/**
 * 语言信息
 */
@Data
public class Language {

    /** 语言名称 */
    private String language;

    /** 文件扩展名 */
    private String extension;

}
