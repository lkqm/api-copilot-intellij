package io.apix.model;

import io.apix.util.OpenApiUtils;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

@Value
public class Property {

    private String name;
    private Schema<?> schema;
    private boolean required;

    public boolean isObjectType() {
        return OpenApiUtils.isObjectType(this.schema);
    }

    public boolean isArrayType() {
        return OpenApiUtils.isArrayType(this.schema);
    }

    public boolean isArrayObjectType() {
        return OpenApiUtils.isArrayType(this.schema) && OpenApiUtils.isObjectType(this.schema.getItems());
    }

    public boolean isMultipleArrayType() {
        return OpenApiUtils.isArrayType(this.schema) && OpenApiUtils.isArrayType(this.schema.getItems());
    }

    public List<Property> getObjectProperties() {
        Map<String, Schema> schemaProperties = schema.getProperties();
        if (schemaProperties == null || schemaProperties.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> required = schema.getRequired() == null ? Collections.emptySet() : new HashSet<>(schema.getRequired());
        return schemaProperties.entrySet().stream()
                .map(entry -> new Property(entry.getKey(), entry.getValue(), required.contains(entry.getKey())))
                .collect(Collectors.toList());
    }

    public List<Property> getArrayObjectProperties() {
        if (schema.getItems() == null || schema.getItems().getProperties() == null) {
            return Collections.emptyList();
        }
        return new Property(null, schema.getItems(), false).getObjectProperties();
    }

}
