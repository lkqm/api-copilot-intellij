package io.apicopilot.codegen.model;

import lombok.Data;

@Data
public class TypeMapping {

    /**
     * OpenAPI type
     */
    private String type;

    /**
     * OpenAPI format
     */
    private String format;

    /**
     * Type in code
     */
    private String targetType;

}
