package io.apix.codegen.core;

import io.apix.codegen.model.TypeMapping;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class TypeResolverImpl implements TypeResolver {

    private final String language;
    private final List<TypeMapping> typeMappings;


    @Override
    public String resolve(String type, String format) {
        List<TypeMapping> mappings = typeMappings.stream()
                .filter(mapping -> Objects.equals(type, mapping.getType())).collect(Collectors.toList());
        return mappings.stream()
                .filter(mapping -> Objects.equals(format, mapping.getFormat()))
                .map(TypeMapping::getTargetType)
                .findFirst()
                .orElseGet(() -> mappings.stream().filter(m -> m.getFormat() == null || m.getFormat().isEmpty()).findFirst().map(TypeMapping::getTargetType).orElse(null));
    }
}
