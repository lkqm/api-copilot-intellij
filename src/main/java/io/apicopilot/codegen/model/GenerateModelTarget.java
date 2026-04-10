package io.apicopilot.codegen.model;

import io.swagger.v3.oas.models.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateModelTarget {
    private Scope scope = Scope.ALL;
    private Schema<?> schema;
    private String defaultTypeName;

    public enum Scope {
        ALL,
        CUSTOM_SCHEMA
    }

    public static GenerateModelTarget all() {
        return new GenerateModelTarget(Scope.ALL, null, null);
    }

    public static GenerateModelTarget customSchema(Schema<?> schema, String defaultTypeName) {
        return new GenerateModelTarget(Scope.CUSTOM_SCHEMA, schema, defaultTypeName);
    }
}
